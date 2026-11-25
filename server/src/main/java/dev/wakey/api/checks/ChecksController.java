package dev.wakey.api.checks;

import dev.wakey.web.ApiPaths;
import dev.wakey.web.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** The read side of the checks HTTP adapter. */
@RestController
final class ChecksController {

    private static final String COLLECTION_PATH = ApiPaths.API_V1 + "/spaces/{slug}/checks";

    private static final String COUNTS_PATH = COLLECTION_PATH + "/counts";

    private static final String ITEM_PATH = COLLECTION_PATH + "/{id}";

    private static final String JSON = "application/json";

    private static final String PROBLEM_JSON = "application/problem+json";

    private static final String UNAUTHENTICATED = "Authentication is required.";

    private final ChecksPages pages;

    private final ChecksCommands commands;

    ChecksController(ChecksPages pages, ChecksCommands commands) {
        this.pages = pages;
        this.commands = commands;
    }

    @Operation(operationId = "listChecks", summary = "List the checks of a space")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "400",
            description = "The cursor is invalid.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = UNAUTHENTICATED,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "The space was not found.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping(path = COLLECTION_PATH, produces = JSON)
    CursorPage<CheckResponse> listChecks(
            @PathVariable String slug, @ParameterObject CheckListQuery query, Authentication authentication) {
        return pages.page(authentication, slug, query);
    }

    @Operation(operationId = "countChecks", summary = "Count the checks of a space by state")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "401",
            description = UNAUTHENTICATED,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "The space was not found.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping(path = COUNTS_PATH, produces = JSON)
    StateCountsResponse countChecks(
            @PathVariable String slug, @ParameterObject CheckFilterQuery query, Authentication authentication) {
        return pages.counts(authentication, slug, query);
    }

    @Operation(operationId = "getCheck", summary = "Read one check of a space with its newest result")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "401",
            description = UNAUTHENTICATED,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "The space or check was not found.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping(path = ITEM_PATH, produces = JSON)
    CheckResponse getCheck(@PathVariable String slug, @PathVariable UUID id, Authentication authentication) {
        return commands.get(authentication, slug, id);
    }
}
