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
        @Nullable String phases) {}
