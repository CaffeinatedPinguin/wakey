package dev.wakey.checks.web;

import dev.wakey.checks.CheckService;
import dev.wakey.spaces.web.SpaceReader;
import dev.wakey.spaces.web.SpaceWriter;
import dev.wakey.web.ApiPaths;
import dev.wakey.web.CursorPage;
import dev.wakey.web.PageLimit;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@RequiredArgsConstructor
final class ChecksController {

    private final CheckService checks;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    CursorPage<CheckResponse> listChecks(
            @PathVariable String slug, @Valid CheckListQuery query, PageLimit pageLimit, SpaceReader reader) {
        var space = reader.space();
        int pageSize = pageLimit.value();
        var rows = checks.list(space.id(), query.toFilter(), query.after(), pageSize + 1);
        return CursorPage.of(rows, pageSize, CheckListCodec::sortValue, CheckResponse::from);
    }

    @RequestMapping(path = "/counts", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    StateCountsResponse countChecks(@PathVariable String slug, @Valid CheckFilterQuery query, SpaceReader reader) {
        var space = reader.space();
        var counts = checks.counts(space.id(), query.toFilter());
        return StateCountsResponse.from(counts);
    }

    @RequestMapping(path = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    CheckResponse getCheck(CheckPath path, SpaceReader reader) {
        var space = reader.space();
        UUID id = path.id();
        return read(space.id(), id);
    }

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    CheckResponse createCheck(@PathVariable String slug, SpaceWriter writer, @Valid @RequestBody CheckRequest request) {
        var space = writer.space();
        var actor = CheckRequest.actorOf(writer);
        var created = checks.create(space.id(), request.toDraft(), actor);
        return CheckResponse.from(created);
    }

    @RequestMapping(path = "/{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    CheckResponse updateCheck(
            CheckPath path, SpaceWriter writer, @RequestParam long version, @Valid @RequestBody CheckRequest request) {
        var space = writer.space();
        var actor = CheckRequest.actorOf(writer);
        UUID id = path.id();
        var updated = checks.update(space.id(), request.toUpdate(id, version), actor);
        return CheckResponse.from(updated);
    }

    @RequestMapping(path = "/{id}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteCheck(CheckPath path, SpaceWriter writer) {
        var space = writer.space();
        var actor = CheckRequest.actorOf(writer);
        checks.delete(space.id(), path.id(), actor);
    }

    @RequestMapping(path = "/{id}/pause", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    CheckResponse pauseCheck(CheckPath path, SpaceWriter writer, @Valid @RequestBody PauseCheckRequest request) {
        var space = writer.space();
        var actor = CheckRequest.actorOf(writer);
        UUID id = path.id();
        checks.pause(space.id(), id, request.reason(), actor);
        return read(space.id(), id);
    }

    @RequestMapping(path = "/{id}/resume", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    CheckResponse resumeCheck(CheckPath path, SpaceWriter writer) {
        var space = writer.space();
        var actor = CheckRequest.actorOf(writer);
        UUID id = path.id();
        checks.resume(space.id(), id, actor);
        return read(space.id(), id);
    }

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
