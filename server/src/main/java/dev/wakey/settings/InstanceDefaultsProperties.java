package dev.wakey.settings;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Instance defaults from {@code wakey.defaults.*}; when present they override the catalog default
 * but stay below a plan value and a space override.
 */
@ConfigurationProperties(prefix = "wakey.defaults")
public record InstanceDefaultsProperties(
        @Nullable String timezone,
        @Nullable String language,
        @Nullable CheckDefaults check) {

    /** The instance defaults of the check settings. */
    public record CheckDefaults(
            @Nullable Integer interval,
            @Nullable Integer timeout,
            @Nullable Integer retries) {}
}
