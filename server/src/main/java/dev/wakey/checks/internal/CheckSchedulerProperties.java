package dev.wakey.checks.internal;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The scheduler switches from {@code wakey.checks.scheduler.*}. The default lives in
 * {@code application.yml}; a missing or non-positive parallelism stops the startup instead of
 * silently running no checks.
 */
@Validated
@ConfigurationProperties(prefix = "wakey.checks.scheduler")
record CheckSchedulerProperties(@Positive int parallelism) {}
