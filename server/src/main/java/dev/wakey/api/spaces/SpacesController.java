package dev.wakey.api.spaces;

import dev.wakey.api.SpacePrincipals;
import dev.wakey.spaces.SpaceService;
import dev.wakey.web.ApiProperties;
import dev.wakey.web.CursorCodec;
import dev.wakey.web.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The HTTP read adapter for spaces; all access rules and validation live in the spaces module. */
@RestController
final class SpacesController {

    private static final String DEFAULT_PAGE_SIZE = "20";

    private static final String JSON = "application/json";

    private static final String PROBLEM_JSON = "application/problem+json";

    private static final int EXTRA_ROW = 1;

    private final SpaceService spaces;

    private final ApiProperties apiProperties;

    SpacesController(SpaceService spaces, ApiProperties apiProperties) {
        this.spaces = spaces;
        this.apiProperties = apiProperties;
    }

    @Operation(operationId = "listSpaces", summary = "List the spaces visible to the caller")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "400",
            description = "The cursor or the limit is invalid.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping(path = SpacesPaths.COLLECTION, produces = JSON)
    CursorPage<SpaceResponse> listSpaces(
            @Parameter(description = "The page size, at least 1; values above the instance maximum are capped.")
                    @RequestParam(defaultValue = DEFAULT_PAGE_SIZE)
                    int limit,
            @RequestParam(required = false) Optional<String> cursor,
            Authentication authentication) {
        return SpacePrincipals.tokenSpaceSlug(authentication)
                .map(slug -> tokenSpacePage(authentication, slug))
                .orElseGet(() -> visibleSpacePage(limit, cursor, authentication));
    }

    private CursorPage<SpaceResponse> tokenSpacePage(Authentication authentication, String slug) {
        var actor = SpacePrincipals.of(authentication);
        var view = spaces.view(actor, slug);
        SpaceResponse response = SpaceResponse.from(view);
        return new CursorPage<>(List.of(response), null);
    }

    private CursorPage<SpaceResponse> visibleSpacePage(
            int limit, Optional<String> cursor, Authentication authentication) {
        var actor = SpacePrincipals.of(authentication);
        int pageSize = apiProperties.capPageSize(limit);
        String afterSlug = cursor.map(SpacesController::sortValue).orElse("");
        var views = spaces.listVisible(actor, afterSlug, pageSize + EXTRA_ROW);
        boolean hasMore = views.size() > pageSize;
        var current = hasMore ? views.subList(0, pageSize) : views;
        var items = current.stream().map(SpaceResponse::from).toList();
        if (!hasMore) {
            return new CursorPage<>(items, null);
        }
        var last = current.get(current.size() - EXTRA_ROW);
        String id = last.id().toString();
        String nextCursor = CursorCodec.encode(last.slug(), id);
        return new CursorPage<>(items, nextCursor);
    }

    @Operation(operationId = "getSpace", summary = "Read one space visible to the caller")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "400",
            description = "The slug is invalid.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "The space was not found.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping(path = SpacesPaths.ITEM, produces = JSON)
    SpaceResponse getSpace(
            @PathVariable @Parameter(schema = @Schema(pattern = SpaceResponse.SLUG_PATTERN)) String slug,
            Authentication authentication) {
        var actor = SpacePrincipals.forSpace(authentication, slug);
        var view = spaces.view(actor, slug);
        return SpaceResponse.from(view);
    }

    private static String sortValue(String encoded) {
        return CursorCodec.decode(encoded).sortValue();
    }
}
