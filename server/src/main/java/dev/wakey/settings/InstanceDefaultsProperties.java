package dev.wakey.settings;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * The instance defaults of the general settings from {@code wakey.defaults.*}; each value is written
 * once, in the annotation. A plan bound may still clamp it, and a space override wins inside the bound.
 */
@ConfigurationProperties(prefix = "wakey.defaults")
public record InstanceDefaultsProperties(
        @DefaultValue("UTC") String timezone,
        @DefaultValue("pl") Language language) {}
