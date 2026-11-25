package dev.wakey.api.checks;

import dev.wakey.checks.CheckChanges;
import dev.wakey.checks.CheckResults;
import dev.wakey.checks.CheckService;
import dev.wakey.checks.NewCheck;
import dev.wakey.spaces.SpaceView;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Reads one check with its newest result and changes the checks of a space. */
@Component
final class ChecksCommands {

    private final CheckService checks;

    private final CheckResults results;

    private final ChecksAccess access;

    ChecksCommands(CheckService checks, CheckResults results, ChecksAccess access) {
        this.checks = checks;
        this.results = results;
        this.access = access;
    }

    CheckResponse get(Authentication authentication, String slug, UUID id) {
        SpaceView space = access.read(authentication, slug);
        return ChecksErrors.mapped(() -> read(space, id));
    }

    CheckResponse create(Authentication authentication, String slug, CreateCheckRequest request) {
        SpaceView space = access.write(authentication, slug);
        NewCheck draft = request.toNewCheck();
        return ChecksErrors.mapped(() -> create(space, draft));
    }

    CheckResponse update(Authentication authentication, String slug, UUID id, UpdateCheckRequest request) {
        SpaceView space = access.write(authentication, slug);
        CheckChanges changes = request.toChanges();
        return ChecksErrors.mapped(() -> update(space, id, changes, request.version()));
    }

    CheckResponse pause(Authentication authentication, String slug, UUID id, PauseCheckRequest request) {
        SpaceView space = access.write(authentication, slug);
        ChecksErrors.run(() -> pause(space, id, request));
        return get(authentication, slug, id);
    }

    CheckResponse resume(Authentication authentication, String slug, UUID id) {
        SpaceView space = access.write(authentication, slug);
        ChecksErrors.run(() -> resume(space, id));
        return get(authentication, slug, id);
    }

    void delete(Authentication authentication, String slug, UUID id) {
        SpaceView space = access.write(authentication, slug);
        ChecksErrors.run(() -> delete(space, id));
    }

    private CheckResponse read(SpaceView space, UUID id) {
        var check = checks.get(space.id(), id);
        var latest = results.latest(id);
        return CheckResponse.from(check, latest.orElse(null));
    }

    private CheckResponse create(SpaceView space, NewCheck draft) {
        var created = checks.create(space.id(), draft);
        return CheckResponse.from(created);
    }

    private CheckResponse update(SpaceView space, UUID id, CheckChanges changes, long version) {
        var updated = checks.update(space.id(), id, changes, version);
        return CheckResponse.from(updated);
    }

    private void pause(SpaceView space, UUID id, PauseCheckRequest request) {
        checks.pause(space.id(), id, request.reason());
    }

    private void resume(SpaceView space, UUID id) {
        checks.resume(space.id(), id);
    }

    private void delete(SpaceView space, UUID id) {
        checks.delete(space.id(), id);
    }
}
