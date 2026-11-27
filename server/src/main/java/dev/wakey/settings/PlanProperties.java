package dev.wakey.settings;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.bind.Name;

/**
 * One configured plan. The external keys are {@code wakey.plans.<name>.*}; the bound keys also
 * provide the plan value for their setting, while {@code max-checks} and
 * {@code api-requests-per-minute} stay plan fields only.
 */
public record PlanProperties(
        @Name("default") boolean defaultPlan,
        @Nullable Integer minInterval,
        @Nullable Integer maxTimeout,
        @Nullable Integer maxRetries,
        @Nullable Integer maxChecks,
        @Nullable Integer apiRequestsPerMinute) {}
