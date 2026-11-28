package dev.wakey.checks;

import dev.wakey.checks.internal.CheckStore;
import dev.wakey.checks.internal.CheckValidator;
import dev.wakey.spaces.SpaceService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The module's public service for definitions and their lifecycle. The space id is the only access
 * scope: a check of another space is indistinguishable from a missing one. Every failure is a
 * domain error carrying an {@link dev.wakey.web.ErrorCode}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckService {

    private final CheckStore store;

    private final CheckAuditLog audit;

    private final SpaceService spaces;

    private final CheckPlan planLimits;

    private final EgressPolicy egressPolicy;

    /** Creates a check in the given space after validation against the plan bounds of its space. */
    @Transactional
    public Check create(UUID spaceId, CheckDraft check, CheckActor actor) {
        CheckDraft validated = CheckValidator.validate(check, egressPolicy);
        spaces.lock(spaceId);
        planLimits.requireForNew(spaceId, validated);
        Check created = store.insert(spaceId, validated);
        audit.created(actor, spaceId, created.id());
        log.info("Created check {} in space {} by {} {}", created.id(), spaceId, actorKind(actor), actor.subject());
        return created;
    }

    /**
     * Replaces the editable fields after validation against the plan bounds; a version other than
     * the expected one is a conflict.
     */
    @Transactional
    public Check update(UUID spaceId, CheckUpdate update, CheckActor actor) {
        Check existing = store.get(spaceId, update.id());
        CheckDraft validated = CheckValidator.validate(update.changes(), egressPolicy);
        planLimits.requireForUpdate(spaceId, validated);
        Check updated = store.update(existing, validated, update.expectedVersion());
        audit.updated(actor, spaceId, existing, validated);
        log.info("Updated check {} in space {} by {} {}", existing.id(), spaceId, actorKind(actor), actor.subject());
        return updated;
    }

    /** Reads one check of the space; a missing or foreign check is {@link dev.wakey.web.CommonError#NOT_FOUND}. */
    public Check get(UUID spaceId, UUID id) {
        return store.get(spaceId, id);
    }

    /**
     * Lists the checks of the space ordered by name and then id, resuming after the cursor. The
     * state filter uses the stored newest state and treats a check without results as UNKNOWN.
     */
    public List<Check> list(UUID spaceId, CheckFilter filter, @Nullable CheckPosition after, int limit) {
        List<Check> checks = store.list(spaceId, filter, after, limit);
        log.debug("Listed {} checks in space {} with limit {}", checks.size(), spaceId, limit);
        return checks;
    }

    /** The newest result of the check, if any. */
    public Optional<CheckResult> latest(UUID checkId) {
        return store.latest(checkId);
    }

    /** Counts the filtered checks by state, using the same filters as {@link #list}. */
    public StateCounts counts(UUID spaceId, CheckFilter filter) {
        return store.counts(spaceId, filter);
    }

    /** Pauses monitoring and stores the reason. */
    @Transactional
    public void pause(UUID spaceId, UUID id, PausedReason reason, CheckActor actor) {
        boolean changed = store.pause(spaceId, id, reason);
        if (changed) {
            audit.paused(actor, spaceId, id, reason);
            log.info(
                    "Paused check {} in space {} with reason {} by {} {}",
                    id,
                    spaceId,
                    reason,
                    actorKind(actor),
                    actor.subject());
        }
    }

    /** Resumes monitoring and clears the pause reason. */
    @Transactional
    public void resume(UUID spaceId, UUID id, CheckActor actor) {
        boolean changed = store.resume(spaceId, id);
        if (changed) {
            audit.resumed(actor, spaceId, id);
            log.info("Resumed check {} in space {} by {} {}", id, spaceId, actorKind(actor), actor.subject());
        }
    }

    /** Deletes the check together with its results and tags. */
    @Transactional
    public void delete(UUID spaceId, UUID id, CheckActor actor) {
        store.delete(spaceId, id);
        audit.deleted(actor, spaceId, id);
        log.info("Deleted check {} in space {} by {} {}", id, spaceId, actorKind(actor), actor.subject());
    }

    /**
     * Applies one action to every selected check in a single transaction under the space lock. A
     * filter action changes nothing unless the matching count equals the confirmed one. A no-op is
     * not counted as affected and the whole action writes one audit entry.
     */
    @Transactional
    public int bulk(UUID spaceId, CheckBulkCommand command, CheckActor actor) {
        spaces.lock(spaceId);
        var targets = store.targets(spaceId, command.ids(), command.filter());
        int matched = targets.size();
        requireMatchingCount(command, matched, spaceId);
        int affected = switch (command.action()) {
            case PAUSE -> store.pauseAll(targets, PausedReason.MANUAL);
            case RESUME -> store.resumeAll(targets);
            case DELETE -> store.deleteAll(targets);
        };
        if (affected > 0) {
            audit.bulk(actor, spaceId, command.action(), affected);
        }
        log.info(
                "Bulk {} affected {} checks in space {} by {} {}",
                command.action(),
                affected,
                spaceId,
                actorKind(actor),
                actor.subject());
        return affected;
    }

    private static void requireMatchingCount(CheckBulkCommand command, int matched, UUID spaceId) {
        Integer expected = command.expectedCount();
        if (expected != null && expected != matched) {
            log.debug(
                    "Bulk {} in space {} rejected: expected {} checks but {} matched",
                    command.action(),
                    spaceId,
                    expected,
                    matched);
        }
        command.requireMatchingCount(matched);
    }

    private static String actorKind(CheckActor actor) {
        return actor.token() ? "token" : "user";
    }
}
