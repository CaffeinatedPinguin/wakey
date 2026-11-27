package dev.wakey.tokens.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Instance limits for API tokens; the owner limit keeps one member from filling a space. */
@ConfigurationProperties(prefix = "wakey.tokens")
public record TokenProperties(@DefaultValue("50") int maxPerOwner) {}
