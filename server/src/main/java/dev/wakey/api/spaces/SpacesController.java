package dev.wakey.api.spaces;

import dev.wakey.api.SpacePrincipals;
import dev.wakey.spaces.PutResult;
import dev.wakey.spaces.SpaceService;
import dev.wakey.web.ApiPaths;
import dev.wakey.web.ApiProperties;
import dev.wakey.web.CursorCodec;
import dev.wakey.web.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The HTTP adapter for spaces; all access rules and validation live in the spaces module. The put
 * operation treats {@code If-None-Match: *} as a create-only precondition and is otherwise
 * last-write-wins.
 */
@RestController
final class SpacesController {

    private static final String COLLECTION_PATH = ApiPaths.API_V1 + "/spaces";

    private static final String ITEM_PATH = COLLECTION_PATH + "/{slug}";

    private static final String DEFAULT_PAGE_SIZE = "20";

    private static final String JSON = "application/json";

    private static final String PROBLEM_JSON = "application/problem+json";

    private static final String IF_NONE_MATCH_ANY = "*";

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
            description = "The cursor is invalid.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping(path = COLLECTION_PATH, produces = JSON)
    CursorPage<SpaceResponse> listSpaces(
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int limit,
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
        int requested = Math.max(1, limit);
        int capped = apiProperties.capPageSize(requested);
        int pageSize = Math.max(1, capped);
        String afterSlug = cursor.map(SpacesController::sortValue).orElse("");
        var views = spaces.listVisible(actor, afterSlug, pageSize + EXTRA_ROW);
        boolean hasMore = views.size() > pageSize;
        var current = hasMore ? views.subList(0, pageSize) : views;
        List<SpaceResponse> items = current.stream().map(SpaceResponse::from).toList();
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
    @GetMapping(path = ITEM_PATH, produces = JSON)
    SpaceResponse getSpace(@PathVariable String slug, Authentication authentication) {
        var actor = SpacePrincipals.forSpace(authentication, slug);
        var view = spaces.view(actor, slug);
        return SpaceResponse.from(view);
    }

    @Operation(
            operationId = "putSpace",
            summary = "Create or rename a space idempotently",
            description = "A rename is last-write-wins until the space carries a version or an If-Match precondition. "
                    + "Send If-None-Match: * to create the space only when the slug is free.")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "400",
            description = "The request is invalid.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "The caller is not allowed to create or rename spaces.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = "The space already exists and If-None-Match: * was sent.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "201",
            description = "The space was created.",
            content = @Content(mediaType = JSON, schema = @Schema(implementation = SpaceResponse.class)))
    @PutMapping(path = ITEM_PATH, produces = JSON)
    ResponseEntity<SpaceResponse> putSpace(
            @PathVariable String slug,
            @RequestHeader(name = "If-None-Match", required = false) Optional<String> ifNoneMatch,
            @Valid @RequestBody PutSpaceRequest request,
            Authentication authentication) {
        var actor = SpacePrincipals.forSpace(authentication, slug);
        boolean createOnly = ifNoneMatch.map(IF_NONE_MATCH_ANY::equals).orElse(false);
        PutResult result =
                createOnly ? spaces.create(actor, slug, request.name()) : spaces.put(actor, slug, request.name());
        String storedSlug = result.space().slug();
        var view = spaces.view(actor, storedSlug);
        SpaceResponse response = SpaceResponse.from(view);
        int status = result.created() ? 201 : 200;
        return ResponseEntity.status(status).body(response);
    }

    private static String sortValue(String encoded) {
        return CursorCodec.decode(encoded).sortValue();
    }
}
