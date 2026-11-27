package dev.wakey.tokens.internal;

import dev.wakey.tokens.TokenOwner;
import dev.wakey.tokens.TokenSummary;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Component;

/** The persistence adapter of the token aggregate: Spring Data JDBC writes and reads the rows. */
@Component
@RequiredArgsConstructor
public class TokenStore {

    private static final Duration RETENTION = Duration.ofDays(30);

    private final TokenRepository tokenRows;

    private final JdbcAggregateTemplate aggregates;

    public void insert(TokenEntity token) {
        aggregates.insert(token);
    }

    public Optional<TokenEntity> findByHash(String tokenHash) {
        return tokenRows.findByTokenHash(tokenHash);
    }

    public void purgeStale(UUID spaceId, TokenOwner owner, Instant now) {
        Instant threshold = now.minus(RETENTION);
        Criteria staleRevoked = Criteria.where("revokedAt").lessThan(threshold);
        Criteria staleExpired = Criteria.where("expiresAt").lessThan(threshold);
        Criteria stale = staleRevoked.or(staleExpired);
        Criteria criteria = ownerCriteria(spaceId, owner);
        criteria = criteria.and(stale);
        aggregates.deleteAllByQuery(Query.query(criteria), TokenEntity.class);
    }

    public long countActive(UUID spaceId, TokenOwner owner, Instant now) {
        Criteria expiresAfter = Criteria.where("expiresAt").greaterThan(now);
        Criteria active = Criteria.where("expiresAt").isNull().or(expiresAfter);
        Criteria criteria = ownerCriteria(spaceId, owner);
        criteria = criteria.and("revokedAt").isNull();
        criteria = criteria.and(active);
        return aggregates.count(Query.query(criteria), TokenEntity.class);
    }

    public List<TokenSummary> list(UUID spaceId, TokenOwner owner, Instant now) {
        Instant threshold = now.minus(RETENTION);
        Criteria revokedAfter = Criteria.where("revokedAt").greaterThanOrEquals(threshold);
        Criteria recentRevoked = Criteria.where("revokedAt").isNull().or(revokedAfter);
        Criteria expiryAfter = Criteria.where("expiresAt").greaterThanOrEquals(threshold);
        Criteria recentExpiry = Criteria.where("expiresAt").isNull().or(expiryAfter);
        Criteria criteria = ownerCriteria(spaceId, owner);
        criteria = criteria.and(recentRevoked);
        criteria = criteria.and(recentExpiry);
        Sort newestFirst = Sort.by("createdAt", "id").descending();
        Query query = Query.query(criteria).sort(newestFirst);
        List<TokenEntity> found = aggregates.findAll(query, TokenEntity.class);
        return found.stream().map(TokenStore::toSummary).toList();
    }

    public void markUsed(UUID tokenId, Instant usedAt) {
        aggregates.update(new TokenUsage(tokenId, usedAt));
    }

    public void revoke(UUID spaceId, TokenOwner owner, UUID tokenId, Instant now) {
        Optional<TokenEntity> found = tokenRows.findById(tokenId);
        found.filter(token -> token.spaceId().equals(spaceId))
                .filter(token -> token.ownerIssuer().equals(owner.issuer()))
                .filter(token -> token.ownerSubject().equals(owner.subject()))
                .filter(token -> token.revokedAt() == null)
                .ifPresent(token -> revokeNow(token, now));
    }

    /**
     * Revokes every active token of one owner in one space; the member-removal listener uses it.
     * The active rows are read with one query and then updated in place, so a token keeps its id,
     * version and dependent rows; the owner's token limit bounds the number of updates.
     */
    public void revokeAll(UUID spaceId, String ownerIssuer, String ownerSubject, Instant now) {
        Criteria criteria = Criteria.where("spaceId").is(spaceId);
        criteria = criteria.and("ownerIssuer").is(ownerIssuer);
        criteria = criteria.and("ownerSubject").is(ownerSubject);
        criteria = criteria.and("revokedAt").isNull();
        List<TokenEntity> active = aggregates.findAll(Query.query(criteria), TokenEntity.class);
        for (TokenEntity token : active) {
            revokeNow(token, now);
        }
    }

    private static Criteria ownerCriteria(UUID spaceId, TokenOwner owner) {
        Criteria criteria = Criteria.where("spaceId").is(spaceId);
        criteria = criteria.and("ownerIssuer").is(owner.issuer());
        return criteria.and("ownerSubject").is(owner.subject());
    }

    private void revokeNow(TokenEntity token, Instant now) {
        TokenEntity revoked = new TokenEntity(
                token.id(),
                token.spaceId(),
                token.ownerIssuer(),
                token.ownerSubject(),
                token.name(),
                token.scope(),
                token.tokenHash(),
                token.expiresAt(),
                token.createdAt(),
                token.lastUsedAt(),
                now);
        aggregates.update(revoked);
    }

    private static TokenSummary toSummary(TokenEntity token) {
        return new TokenSummary(
                token.id(),
                token.name(),
                token.scope(),
                token.createdAt(),
                token.expiresAt(),
                token.lastUsedAt(),
                token.revokedAt());
    }
}
