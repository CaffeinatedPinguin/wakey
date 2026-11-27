package dev.wakey.tokens;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * What a token may do and how long it lives. The property stays a separate value because the
 * checkstyle rule caps a method at four parameters.
 */
public record TokenOptions(TokenScope scope, @Nullable Instant expiresAt) {}
