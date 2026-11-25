package dev.wakey.api.checks;

import dev.wakey.checks.Check;
import dev.wakey.checks.CheckChanges;
import dev.wakey.checks.CheckResult;
import dev.wakey.checks.CheckResults;
import dev.wakey.checks.CheckService;
import dev.wakey.checks.NewCheck;
import dev.wakey.spaces.SpaceView;
import java.util.Optional;
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
        return read(space, id);
    }

    CheckResponse create(Authentication authentication, String slug, CreateCheckRequest request) {
        SpaceView space = access.write(authentication, slug);
        NewCheck draft = request.toNewCheck();
        Check created = checks.create(space.id(), draft);
        return CheckResponse.from(created);
    }

    CheckResponse update(Authentication authentication, String slug, UUID id, UpdateCheckRequest request) {
        SpaceView space = access.write(authentication, slug);
        CheckChanges changes = request.toChanges();
        Check updated = checks.update(space.id(), id, changes, request.version());
        return CheckResponse.from(updated);
    }

    CheckResponse pause(Authentication authentication, String slug, UUID id, PauseCheckRequest request) {
        SpaceView space = access.write(authentication, slug);
        checks.pause(space.id(), id, request.reason());
        return read(space, id);
    }

    CheckResponse resume(Authentication authentication, String slug, UUID id) {
        SpaceView space = access.write(authentication, slug);
        checks.resume(space.id(), id);
        return read(space, id);
    }

    void delete(Authentication authentication, String slug, UUID id) {
        SpaceView space = access.write(authentication, slug);
        checks.delete(space.id(), id);
    }

    private CheckResponse read(SpaceView space, UUID id) {
        Check check = checks.get(space.id(), id);
        Optional<CheckResult> latest = results.latest(id);
        return CheckResponse.from(check, latest.orElse(null));
    }
}
