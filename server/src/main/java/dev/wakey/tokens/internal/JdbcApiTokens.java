package dev.wakey.tokens.internal;

import dev.wakey.spaces.SpaceView;
import dev.wakey.tokens.ApiTokens;
import dev.wakey.tokens.CreatedToken;
import dev.wakey.tokens.InvalidTokenException;
import dev.wakey.tokens.TokenIdentity;
import dev.wakey.tokens.TokenLimitException;
import dev.wakey.tokens.TokenOptions;
import dev.wakey.tokens.TokenOwner;
import dev.wakey.tokens.TokenScope;
import dev.wakey.tokens.TokenSummary;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

@RequiredArgsConstructor
final class JdbcApiTokens implements ApiTokens {

    private static final int MAX_NAME_LENGTH = 100;

    private static final int LAST_USED_REFRESH_SECONDS = 60;

    private final ApiTokenStore store;

    private final SpaceAccess spaces;

    private final Clock clock;

    private final int maxPerOwner;

    @Override
    public CreatedToken create(TokenOwner owner, String spaceSlug, String name, TokenOptions options) {
        String checkedName = requireValidName(name);
        Instant now = Instant.now(clock);
        @Nullable Instant expiresAt = options.expiresAt();
        requireFutureExpiry(expiresAt, now);
        SpaceView space = spaces.requireMember(spaceSlug, owner.subject());
        TokenScope scope = options.scope();
        SpaceAccess.requireWriteTokenAllowed(space, scope);
        requireTokenLimit(space, owner, now);
        String secret = TokenSecrets.generate();
        String tokenHash = TokenSecrets.hash(secret);
        NewToken newToken = new NewToken(
                UUID.randomUUID(), space.id(), owner.subject(), checkedName, scope, tokenHash, expiresAt, now);
        store.insert(newToken);
        TokenSummary summary = new TokenSummary(newToken.id(), checkedName, scope, now, expiresAt, null, null);
        return new CreatedToken(secret, summary);
    }

    @Override
    public Optional<TokenIdentity> authenticate(String presentedSecret) {
        if (!presentedSecret.startsWith(TokenSecrets.PREFIX)) {
            return Optional.empty();
        }
        Optional<StoredToken> found = store.findByHash(TokenSecrets.hash(presentedSecret));
        return found.flatMap(token -> authenticateStored(token, Instant.now(clock)));
    }

    @Override
    public List<TokenSummary> list(TokenOwner owner, String spaceSlug) {
        SpaceView space = spaces.requireMember(spaceSlug, owner.subject());
        return store.list(space.id(), owner.subject());
    }

    @Override
    public void revoke(TokenOwner owner, String spaceSlug, UUID tokenId) {
        SpaceView space = spaces.requireMember(spaceSlug, owner.subject());
        store.revoke(space.id(), owner.subject(), tokenId, Instant.now(clock));
    }

    private Optional<TokenIdentity> authenticateStored(StoredToken token, Instant now) {
        Optional<TokenIdentity> identity = Optional.empty();
        if (isUsable(token, now)) {
            identity = spaces.findMembership(token.spaceSlug(), token.ownerSubject())
                    .filter(membership -> joinedBeforeToken(membership, token))
                    .map(membership -> useToken(token, membership, now));
        }
        return identity;
    }

    private TokenIdentity useToken(StoredToken token, SpaceAccess.Membership membership, Instant now) {
        store.markUsed(token.id(), now, now.minusSeconds(LAST_USED_REFRESH_SECONDS));
        TokenScope scope = membership.effectiveScope(token.scope());
        return new TokenIdentity(token.id(), token.spaceId(), token.spaceSlug(), token.ownerSubject(), scope);
    }

    private static boolean joinedBeforeToken(SpaceAccess.Membership membership, StoredToken token) {
        return !token.createdAt().isBefore(membership.since());
    }

    private void requireTokenLimit(SpaceView space, TokenOwner owner, Instant now) {
        long active = store.countActive(space.id(), owner.subject(), now);
        if (active >= maxPerOwner) {
            throw new TokenLimitException(maxPerOwner);
        }
    }

    private static void requireFutureExpiry(@Nullable Instant expiresAt, Instant now) {
        if (expiresAt != null && !expiresAt.isAfter(now)) {
            throw new InvalidTokenException("expiresAt", "invalid_expiry", "A token expiry must be in the future");
        }
    }

    private static boolean isUsable(StoredToken token, Instant now) {
        return token.revokedAt() == null && notExpired(token.expiresAt(), now);
    }

    private static boolean notExpired(@Nullable Instant expiresAt, Instant now) {
        return expiresAt == null || expiresAt.isAfter(now);
    }

    private static String requireValidName(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH) {
            throw new InvalidTokenException("name", "invalid_name", "A token name must have 1-100 characters");
        }
        return trimmed;
    }
}
