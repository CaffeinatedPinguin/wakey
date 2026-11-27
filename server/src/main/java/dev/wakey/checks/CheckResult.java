package dev.wakey.checks;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * One measurement of a check. {@code locationId} may be null when the caller does not name a
 * location; the stored result then uses the server location.
 */
public record CheckResult(
        @Nullable String locationId,
        Instant checkedAt,
        CheckState state,
        @Nullable ErrorCategory errorCategory,
        @Nullable Integer httpStatus,
        @Nullable Integer durationMs,
        @Nullable String protocolVersion,
        @Nullable String phases) {

    /** The location used when the caller does not name one. */
    public static final String SERVER_LOCATION = "server";

    /** Returns this result with the given location. */
    public CheckResult withLocation(String location) {
        return new CheckResult(
                location, checkedAt, state, errorCategory, httpStatus, durationMs, protocolVersion, phases);
    }

    /** Returns this result with the given state. */
    public CheckResult withState(CheckState newState) {
        return new CheckResult(
                locationId, checkedAt, newState, errorCategory, httpStatus, durationMs, protocolVersion, phases);
    }
}
