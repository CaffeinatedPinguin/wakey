package dev.wakey.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The instance defaults of the general settings from {@code wakey.defaults.*}; the values are
 * written only in {@code application.yml}, so a missing value stops the startup instead of silently
 * falling back to {@code null}. A plan bound may still clamp them, and a space override wins inside
 * the bound.
 */
@Validated
@ConfigurationProperties(prefix = "wakey.defaults")
public record InstanceDefaultsProperties(
        @NotBlank String timezone, @NotNull Language language) {}
