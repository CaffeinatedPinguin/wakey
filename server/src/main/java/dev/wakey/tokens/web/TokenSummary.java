package dev.wakey.tokens.web;

import dev.wakey.tokens.TokenScope;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** The contract-facing token summary, so the domain record stays free of API annotations. */
@Schema(description = "A token as shown to its owner")
public record TokenSummary(
        UUID id,
        String name,

        @Schema(implementation = TokenScope.class, enumAsRef = true)
        TokenScope scope,

        Instant createdAt,
        @Nullable Instant expiresAt,
        @Nullable Instant lastUsedAt,
        @Nullable Instant revokedAt) {

    static TokenSummary from(dev.wakey.tokens.TokenSummary summary) {
        return new TokenSummary(
                summary.id(),
                summary.name(),
                summary.scope(),
                summary.createdAt(),
                summary.expiresAt(),
                summary.lastUsedAt(),
                summary.revokedAt());
    }
}
