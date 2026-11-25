package dev.wakey.checks;

import org.jspecify.annotations.Nullable;

/**
 * The bounds the assigned plan puts on a check; a null field means the plan does not limit that
 * value, and {@code planName} names the plan in a rejection.
 */
public record PlanLimits(
        @Nullable Integer minInterval,
        @Nullable Integer maxTimeout,
        @Nullable Integer maxRetries,
        @Nullable Integer maxChecks,
        String planName) {}
