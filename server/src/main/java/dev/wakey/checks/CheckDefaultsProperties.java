package dev.wakey.checks;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * The instance defaults of the check settings from {@code wakey.defaults.check.*}; each value is
 * written once, in the annotation.
 */
@ConfigurationProperties(prefix = "wakey.defaults.check")
public record CheckDefaultsProperties(
        @DefaultValue("60") int interval,
        @DefaultValue("10") int timeout,
        @DefaultValue("1") int retries) {}
