package dev.wakey.api.spaces;

import dev.wakey.spaces.SpaceService;
import dev.wakey.spaces.SpaceSlug;
import dev.wakey.web.ApiException;
import dev.wakey.web.ApiProperties;
import dev.wakey.web.CommonError;
import dev.wakey.web.CursorCodec;
import dev.wakey.web.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The HTTP read adapter for spaces; all access rules and validation live in the spaces module. */
@RestController
@RequiredArgsConstructor
final class SpacesController {

    private static final String DEFAULT_PAGE_SIZE = "20";

    private static final int EXTRA_ROW = 1;

    private final SpaceService spaces;

    private final ApiProperties apiProperties;

    @Operation(operationId = "listSpaces", summary = "List the spaces visible to the caller")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "400",
            description = "The cursor or the limit is invalid.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping(path = SpacesPaths.COLLECTION, produces = MediaType.APPLICATION_JSON_VALUE)
    CursorPage<SpaceResponse> listSpaces(
            @Parameter(description = "The page size, at least 1; values above the instance maximum are capped.")
                    @RequestParam(defaultValue = DEFAULT_PAGE_SIZE)
                    int limit,
            @RequestParam(required = false) Optional<String> cursor,
            Authentication authentication) {
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
        String nextCursor = CursorCodec.encode(last.slug());
        return new CursorPage<>(items, nextCursor);
    }

    @Operation(operationId = "getSpace", summary = "Read one space visible to the caller")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "400",
            description = "The slug is invalid.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "The space was not found.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping(path = SpacesPaths.ITEM, produces = MediaType.APPLICATION_JSON_VALUE)
    SpaceResponse getSpace(
            @PathVariable @Parameter(schema = @Schema(pattern = SpaceResponse.SLUG_PATTERN)) String slug,
            Authentication authentication) {
        var actor = SpacePrincipals.of(authentication);
        return SpaceResponse.from(spaces.view(actor, slug));
    }

    private static String sortValue(String encoded) {
        String sortValue = CursorCodec.decode(encoded);
        if (!SpaceSlug.isValid(sortValue)) {
            throw new ApiException(CommonError.INVALID_CURSOR);
        }
        return sortValue;
    }
}
