package dev.wakey.tokens.internal;

import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceView;
import dev.wakey.tokens.ApiTokens;
import dev.wakey.tokens.CreatedToken;
import dev.wakey.tokens.InvalidTokenException;
import dev.wakey.tokens.TokenIdentity;
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

    private final ApiTokenStore store;

    private final SpaceAccess spaces;

    private final Clock clock;

    @Override
    public CreatedToken create(TokenOwner owner, String spaceSlug, String name, TokenOptions options) {
        String checkedName = requireValidName(name);
        SpaceView space = spaces.requireMember(spaceSlug, owner.subject());
        TokenScope scope = options.scope();
        @Nullable Instant expiresAt = options.expiresAt();
        SpaceAccess.requireWriteTokenAllowed(space, scope);
        String secret = TokenSecrets.generate();
        Instant createdAt = Instant.now(clock);
        String tokenHash = TokenSecrets.hash(secret);
        NewToken newToken = new NewToken(
                UUID.randomUUID(), space.id(), owner.subject(), checkedName, scope, tokenHash, expiresAt, createdAt);
        store.insert(newToken);
        TokenSummary summary = new TokenSummary(newToken.id(), checkedName, scope, createdAt, expiresAt, null, null);
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
        if (!isUsable(token, now)) {
            return Optional.empty();
        }
        Optional<TokenScope> effectiveScope = spaces.findRole(token.spaceSlug(), token.ownerSubject())
                .map(role -> effectiveScope(token.scope(), role));
        Optional<TokenIdentity> identity = Optional.empty();
        if (effectiveScope.isPresent()) {
            store.markUsed(token.id(), now);
            TokenScope scope = effectiveScope.get();
            TokenIdentity resolved =
                    new TokenIdentity(token.id(), token.spaceId(), token.spaceSlug(), token.ownerSubject(), scope);
            identity = Optional.of(resolved);
        }
        return identity;
    }

    private static boolean isUsable(StoredToken token, Instant now) {
        return token.revokedAt() == null && notExpired(token.expiresAt(), now);
    }

    private static boolean notExpired(@Nullable Instant expiresAt, Instant now) {
        return expiresAt == null || expiresAt.isAfter(now);
    }

    private static TokenScope effectiveScope(TokenScope tokenScope, SpaceRole role) {
        return role == SpaceRole.VIEWER ? TokenScope.READ : tokenScope;
    }

    private static String requireValidName(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH) {
            throw new InvalidTokenException("name", "invalid_name", "A token name must have 1-100 characters");
        }
        return trimmed;
    }
}
