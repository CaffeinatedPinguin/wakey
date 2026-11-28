package dev.wakey.checks.web;

import dev.wakey.checks.CheckService;
import dev.wakey.spaces.web.SpaceReader;
import dev.wakey.spaces.web.SpaceWriter;
import dev.wakey.web.ApiPaths;
import dev.wakey.web.ApiProperties;
import dev.wakey.web.CursorPage;
import dev.wakey.web.PageQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The single HTTP adapter of the checks resource: every operation maps its arguments to the service
 * and the DTO factories, while all rules live in {@link CheckService}.
 */
@RestController
@RequestMapping(ApiPaths.API_V1 + "/spaces/{slug}/checks")
@Tag(name = "checks")
@RequiredArgsConstructor
final class ChecksController {

    private static final String INVALID_REQUEST = "The request is invalid.";

    private static final String FORBIDDEN = "The role does not allow changing checks in this space.";

    private static final String INVALID_CURSOR = "The cursor is invalid.";

    private static final String CONFLICT = "The check was changed by someone else.";

    private static final String COUNT_CHANGED = "The number of matching checks changed since the dialog was shown.";

    private final CheckService checks;

    private final ApiProperties apiProperties;

    @Operation(summary = "List the checks of a space")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "400",
            description = INVALID_CURSOR,
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    CursorPage<CheckResponse> listChecks(
            @PathVariable String slug, @Valid PageQuery page, @Valid CheckFilterQuery query, SpaceReader reader) {
        var space = reader.space();
        int pageSize = page.pageSize(apiProperties);
        var after = CheckListCodec.decode(page.cursor());
        var rows = checks.list(space.id(), query.toFilter(), after, pageSize + 1);
        return CursorPage.of(rows, pageSize, CheckListCodec::sortValue, CheckResponse::from);
    }

    @Operation(summary = "Count the checks of a space by state")
    @RequestMapping(path = "/counts", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    StateCountsResponse countChecks(@PathVariable String slug, @Valid CheckFilterQuery query, SpaceReader reader) {
        var space = reader.space();
        var counts = checks.counts(space.id(), query.toFilter());
        return StateCountsResponse.from(counts);
    }

    @Operation(summary = "Read one check of a space with its newest result")
    @RequestMapping(path = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    CheckResponse getCheck(CheckPath path, SpaceReader reader) {
        var space = reader.space();
        UUID id = path.id();
        return read(space.id(), id);
    }

    @Operation(summary = "Create a check in a space")
    @ApiResponse(
            responseCode = "400",
            description = INVALID_REQUEST,
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = FORBIDDEN,
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    CheckResponse createCheck(@PathVariable String slug, SpaceWriter writer, @Valid @RequestBody CheckRequest request) {
        var space = writer.space();
        var actor = CheckRequest.actorOf(writer);
        var created = checks.create(space.id(), request.toDraft(), actor);
        return CheckResponse.from(created);
    }

    @Operation(summary = "Replace a check, rejecting a stale version")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "400",
            description = INVALID_REQUEST,
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = FORBIDDEN,
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = CONFLICT,
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @RequestMapping(path = "/{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    CheckResponse updateCheck(
            CheckPath path, SpaceWriter writer, @RequestParam long version, @Valid @RequestBody CheckRequest request) {
        var space = writer.space();
        var actor = CheckRequest.actorOf(writer);
        UUID id = path.id();
        var updated = checks.update(space.id(), request.toUpdate(id, version), actor);
        return CheckResponse.from(updated);
    }

    @Operation(summary = "Delete a check together with its results and tags")
    @ApiResponse(
            responseCode = "403",
            description = FORBIDDEN,
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @RequestMapping(path = "/{id}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteCheck(CheckPath path, SpaceWriter writer) {
        var space = writer.space();
        var actor = CheckRequest.actorOf(writer);
        checks.delete(space.id(), path.id(), actor);
    }

    @Operation(summary = "Pause monitoring of a check")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "400",
            description = INVALID_REQUEST,
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = FORBIDDEN,
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @RequestMapping(path = "/{id}/pause", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    CheckResponse pauseCheck(CheckPath path, SpaceWriter writer, @Valid @RequestBody PauseCheckRequest request) {
        var space = writer.space();
        var actor = CheckRequest.actorOf(writer);
        UUID id = path.id();
        checks.pause(space.id(), id, request.reason(), actor);
        return read(space.id(), id);
    }

    @Operation(summary = "Resume monitoring of a check")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "403",
            description = FORBIDDEN,
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @RequestMapping(path = "/{id}/resume", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    CheckResponse resumeCheck(CheckPath path, SpaceWriter writer) {
        var space = writer.space();
        var actor = CheckRequest.actorOf(writer);
        UUID id = path.id();
        checks.resume(space.id(), id, actor);
        return read(space.id(), id);
    }

    @Operation(summary = "Pause, resume or delete many checks at once")
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "400",
            description = INVALID_REQUEST,
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = FORBIDDEN,
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = COUNT_CHANGED,
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @RequestMapping(path = "/bulk", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    BulkChecksResponse bulkChecks(
            @PathVariable String slug, SpaceWriter writer, @Valid @RequestBody BulkChecksRequest request) {
        var space = writer.space();
        var actor = CheckRequest.actorOf(writer);
        var affected = checks.bulk(space.id(), request.toCommand(), actor);
        return BulkChecksResponse.from(affected);
    }

    private CheckResponse read(UUID spaceId, UUID id) {
        var check = checks.get(spaceId, id);
        var latest = checks.latest(id);
        return CheckResponse.from(check, latest.orElse(null));
    }
}
