package dev.wakey.tokens.internal;

import dev.wakey.tokens.TokenScope;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** One token row; the hash, never the secret, is stored and every column keeps its schema name. */
@Table("api_token")
public record TokenEntity(
        @Id UUID id,
        UUID spaceId,
        String ownerIssuer,
        String ownerSubject,
        String name,
        TokenScope scope,
        String tokenHash,
        @Nullable Instant expiresAt,
        Instant createdAt,
        @Nullable Instant lastUsedAt,
        @Nullable Instant revokedAt) {}
