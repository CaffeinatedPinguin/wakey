package dev.wakey.checks;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The instance defaults of the check settings from {@code wakey.defaults.check.*}; the values are
 * written only in {@code application.yml}, so a missing value stops the startup instead of silently
 * falling back to zero.
 */
@Validated
@ConfigurationProperties(prefix = "wakey.defaults.check")
public record CheckDefaultsProperties(
        @Positive int interval,
        @Positive int timeout,
        @PositiveOrZero @Max(CheckLimits.MAX_RETRIES) int retries) {}
