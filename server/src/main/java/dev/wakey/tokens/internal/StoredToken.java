package dev.wakey.tokens.internal;

import dev.wakey.tokens.TokenScope;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** One stored token with its space slug, enough to re-check the owner's role on every use. */
record StoredToken(
        UUID id,
        UUID spaceId,
        String ownerSubject,
        TokenScope scope,
        @Nullable Instant expiresAt,
        @Nullable Instant revokedAt,
        String spaceSlug) {}
