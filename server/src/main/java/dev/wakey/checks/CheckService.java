package dev.wakey.checks;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * The module's public port for definitions and their lifecycle. The space id is the only access
 * scope: a check of another space is indistinguishable from a missing one. The HTTP layer maps the
 * domain exceptions to Problem Details.
 */
public interface CheckService {

    /** Creates a check in the given space after validation against the plan bounds. */
    Check create(UUID spaceId, CheckDraft check, PlanLimits limits);

    /**
     * Replaces the editable fields after validation against the plan bounds; a version other than
     * the expected one is a conflict.
     */
    Check update(UUID spaceId, CheckUpdate update);

    /** Reads one check of the space; a missing or foreign check is {@link CheckNotFoundException}. */
    Check get(UUID spaceId, UUID id);

    /**
     * Lists the checks of the space ordered by name and then id, resuming after the cursor. The
     * state filter uses the latest result and treats a check without results as UNKNOWN.
     */
    CheckPage list(UUID spaceId, CheckFilter filter, @Nullable CheckCursor after, int limit);

    /** Counts the filtered checks by state, using the same filters as {@link #list}. */
    StateCounts counts(UUID spaceId, CheckFilter filter);

    /** Pauses monitoring and stores the reason. */
    void pause(UUID spaceId, UUID id, PausedReason reason);

    /** Resumes monitoring and clears the pause reason. */
    void resume(UUID spaceId, UUID id);

    /** Deletes the check together with its results and tags. */
    void delete(UUID spaceId, UUID id);
}
