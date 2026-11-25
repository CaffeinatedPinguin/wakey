package dev.wakey.checks;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The append-only results of checks. */
public interface CheckResults {

    /**
     * Appends one result. A {@link CheckState#DOWN} state with the
     * {@link ErrorCategory#LOCATION_NETWORK} category is rejected, because a location that cannot
     * reach the network does not prove the target is down.
     */
    void record(UUID checkId, CheckResult result);

    /** The newest result of the check, if any. */
    Optional<CheckResult> latest(UUID checkId);

    /** The newest results of the check, newest first. */
    List<CheckResult> recent(UUID checkId, int limit);
}
