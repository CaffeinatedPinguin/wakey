package dev.wakey.tokens;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** A token as shown to its owner; it never carries the secret or the stored hash. */
public record TokenSummary(
        UUID id,
        String name,
        TokenScope scope,
        Instant createdAt,
        @Nullable Instant expiresAt,
        @Nullable Instant lastUsedAt,
        @Nullable Instant revokedAt) {}
