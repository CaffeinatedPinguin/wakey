package dev.wakey.api.checks;

import dev.wakey.web.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The write side of the checks HTTP adapter. */
@RestController
final class ChecksWriteController {

    private static final String COLLECTION_PATH = ApiPaths.API_V1 + "/spaces/{slug}/checks";

    private static final String ITEM_PATH = COLLECTION_PATH + "/{id}";

    private static final String JSON = "application/json";

    private static final String PROBLEM_JSON = "application/problem+json";

    private static final String UNAUTHENTICATED = "Authentication is required.";

    private static final String FORBIDDEN = "The role does not allow changing checks in this space.";

    private static final String SPACE_NOT_FOUND = "The space was not found.";

    private final ChecksCommands commands;

    ChecksWriteController(ChecksCommands commands) {
        this.commands = commands;
    }

    @Operation(operationId = "createCheck", summary = "Create a check in a space")
    @ApiResponse(responseCode = "201", description = "The check was created.", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "400",
            description = "The request is invalid.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = UNAUTHENTICATED,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = FORBIDDEN,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = SPACE_NOT_FOUND,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(path = COLLECTION_PATH, produces = JSON)
    CheckResponse createCheck(
            @PathVariable String slug, @Valid @RequestBody CreateCheckRequest request, Authentication authentication) {
        return commands.create(authentication, slug, request);
    }

    @Operation(operationId = "updateCheck", summary = "Replace a check, rejecting a stale version")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "400",
            description = "The request is invalid.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = UNAUTHENTICATED,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = FORBIDDEN,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "The space or check was not found.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = "The check was changed by someone else.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping(path = ITEM_PATH, produces = JSON)
    CheckResponse updateCheck(
            @PathVariable String slug,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCheckRequest request,
            Authentication authentication) {
        return commands.update(authentication, slug, id, request);
    }

    @Operation(operationId = "pauseCheck", summary = "Pause monitoring of a check")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "400",
            description = "The request is invalid.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = UNAUTHENTICATED,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = FORBIDDEN,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "The space or check was not found.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping(path = ITEM_PATH + "/pause", produces = JSON)
    CheckResponse pauseCheck(
            @PathVariable String slug,
            @PathVariable UUID id,
            @Valid @RequestBody PauseCheckRequest request,
            Authentication authentication) {
        return commands.pause(authentication, slug, id, request);
    }

    @Operation(operationId = "resumeCheck", summary = "Resume monitoring of a check")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "401",
            description = UNAUTHENTICATED,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = FORBIDDEN,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "The space or check was not found.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping(path = ITEM_PATH + "/resume", produces = JSON)
    CheckResponse resumeCheck(@PathVariable String slug, @PathVariable UUID id, Authentication authentication) {
        return commands.resume(authentication, slug, id);
    }

    @Operation(operationId = "deleteCheck", summary = "Delete a check together with its results and tags")
    @ApiResponse(responseCode = "204", description = "The check was deleted.")
    @ApiResponse(
            responseCode = "401",
            description = UNAUTHENTICATED,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = FORBIDDEN,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "The space or check was not found.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(ITEM_PATH)
    void deleteCheck(@PathVariable String slug, @PathVariable UUID id, Authentication authentication) {
        commands.delete(authentication, slug, id);
    }
}
