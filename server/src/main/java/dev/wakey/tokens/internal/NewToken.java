package dev.wakey.tokens.internal;

import dev.wakey.tokens.TokenScope;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** The values a new token needs; the hash, never the secret, goes to the database. */
record NewToken(
        UUID id,
        UUID spaceId,
        String ownerSubject,
        String name,
        TokenScope scope,
        String tokenHash,
        @Nullable Instant expiresAt,
        Instant createdAt) {}
