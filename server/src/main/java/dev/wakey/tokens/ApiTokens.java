package dev.wakey.tokens;

import dev.wakey.spaces.SpaceService;
import dev.wakey.tokens.internal.SpaceAccess;
import dev.wakey.tokens.internal.TokenEntity;
import dev.wakey.tokens.internal.TokenProperties;
import dev.wakey.tokens.internal.TokenSecrets;
import dev.wakey.tokens.internal.TokenStore;
import dev.wakey.tokens.internal.TokenValidation;
import dev.wakey.web.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The module's public service. A token belongs to one space and one owner, and its effective scope
 * never exceeds the owner's current role in that space. Every failure is an
 * {@link dev.wakey.web.ApiException}; a caller that cannot see the space gets the spaces module's
 * not-found error.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiTokens {

    private static final int LAST_USED_REFRESH_SECONDS = 60;

    private final TokenStore store;

    private final SpaceAccess spaces;

    private final SpaceService spaceService;

    private final Clock clock;

    private final TokenProperties properties;

    /**
     * Creates a token and returns its secret once; only the SHA-256 hash is stored. The secret is
     * never part of the {@link CreatedToken#toString()}. A viewer may only create a read token.
     */
    @Transactional
    public CreatedToken create(TokenOwner owner, String spaceSlug, String name, TokenOptions options) {
        String checkedName = TokenValidation.requireValidName(name);
        Instant now = Instant.now(clock);
        TokenValidation.requireFutureExpiry(options.expiresAt(), now);
        var space = spaces.requireMember(spaceSlug, owner.issuer(), owner.subject());
        TokenScope scope = options.scope();
        SpaceAccess.requireWriteTokenAllowed(space, scope);
        requireTokenLimit(space.id(), owner, now);
        String secret = TokenSecrets.generate();
        String tokenHash = TokenSecrets.hash(secret);
        TokenEntity token = new TokenEntity(
                UUID.randomUUID(),
                space.id(),
                owner.issuer(),
                owner.subject(),
                checkedName,
                scope,
                tokenHash,
                options.expiresAt(),
                now,
                null,
                null);
        store.insert(token);
        TokenSummary summary = new TokenSummary(token.id(), checkedName, scope, now, options.expiresAt(), null, null);
        log.info(
                "Created {} token {} for subject {} in space {} expiring at {}",
                scope,
                token.id(),
                owner.subject(),
                space.id(),
                options.expiresAt());
        return new CreatedToken(secret, summary);
    }

    /**
     * Resolves a presented secret to its token, or an empty result for a wrong prefix, an unknown,
     * expired or revoked token, or an owner that is no longer a member of the space. A successful
     * call records the use.
     */
    public Optional<TokenIdentity> authenticate(String presentedSecret) {
        Instant now = Instant.now(clock);
        boolean prefixed = presentedSecret.startsWith(TokenSecrets.PREFIX);
        Optional<TokenEntity> found =
                prefixed ? store.findByHash(TokenSecrets.hash(presentedSecret)) : Optional.empty();
        if (!prefixed) {
            log.debug("Rejected API token: unknown prefix");
        } else if (found.isEmpty()) {
            log.debug("Rejected API token: unknown");
        }
        return found.flatMap(token -> authenticateStored(token, now));
    }

    /** Lists every token of the owner in a space, revoked and expired ones included. */
    public List<TokenSummary> list(TokenOwner owner, String spaceSlug) {
        var space = spaces.requireMember(spaceSlug, owner.issuer(), owner.subject());
        List<TokenSummary> tokens = store.list(space.id(), owner);
        int count = tokens.size();
        log.debug("Listed {} tokens for subject {} in space {}", count, owner.subject(), space.id());
        return tokens;
    }

    /** Revokes the owner's token in a space; revoking an already revoked token changes nothing. */
    public void revoke(TokenOwner owner, String spaceSlug, UUID tokenId) {
        var space = spaces.requireMember(spaceSlug, owner.issuer(), owner.subject());
        store.revoke(space.id(), owner, tokenId, Instant.now(clock));
    }

    private Optional<TokenIdentity> authenticateStored(TokenEntity token, Instant now) {
        Optional<String> rejection = rejectionReason(token, now);
        if (rejection.isPresent()) {
            return reject(token, rejection.get());
        }
        return memberIdentity(token, now);
    }

    private Optional<TokenIdentity> memberIdentity(TokenEntity token, Instant now) {
        Optional<SpaceAccess.Membership> membership =
                spaces.findMembership(token.spaceId(), token.ownerIssuer(), token.ownerSubject());
        if (membership.isEmpty()) {
            return reject(token, "owner is not a member");
        }
        SpaceAccess.Membership found = membership.get();
        return Optional.of(useToken(token, found, now));
    }

    private static Optional<TokenIdentity> reject(TokenEntity token, String reason) {
        log.debug("Rejected API token {} in space {}: {}", token.id(), token.spaceId(), reason);
        return Optional.empty();
    }

    private static Optional<String> rejectionReason(TokenEntity token, Instant now) {
        Optional<String> reason = Optional.empty();
        if (token.revokedAt() != null) {
            reason = Optional.of("revoked");
        } else if (isExpired(token, now)) {
            reason = Optional.of("expired");
        }
        return reason;
    }

    private static boolean isExpired(TokenEntity token, Instant now) {
        var expiresAt = token.expiresAt();
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    private TokenIdentity useToken(TokenEntity token, SpaceAccess.Membership membership, Instant now) {
        var lastUsedAt = token.lastUsedAt();
        boolean refresh = lastUsedAt == null || lastUsedAt.isBefore(now.minusSeconds(LAST_USED_REFRESH_SECONDS));
        if (refresh) {
            store.markUsed(token.id(), now);
        } else {
            log.debug("Skipping last_used_at update for token {}: refreshed recently", token.id());
        }
        TokenScope scope = membership.effectiveScope(token.scope());
        log.debug("Authenticated token {} for space {}", token.id(), membership.slug());
        return new TokenIdentity(
                token.id(), token.spaceId(), membership.slug(), token.ownerIssuer(), token.ownerSubject(), scope);
    }

    private void requireTokenLimit(UUID spaceId, TokenOwner owner, Instant now) {
        spaceService.lock(spaceId);
        int maxPerOwner = properties.maxPerOwner();
        long active = store.countActive(spaceId, owner, now);
        if (active >= maxPerOwner) {
            log.debug(
                    "Space {} reached the token limit of {} for subject {} with {} active tokens",
                    spaceId,
                    maxPerOwner,
                    owner.subject(),
                    active);
            Map<String, Object> params = Map.of("bound", maxPerOwner);
            throw new ApiException(TokenError.TOKEN_LIMIT, params);
        }
    }
}
