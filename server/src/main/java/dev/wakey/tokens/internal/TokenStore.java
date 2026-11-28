package dev.wakey.tokens.internal;

import dev.wakey.audit.ActorKind;
import dev.wakey.audit.AuditActor;
import dev.wakey.audit.AuditChange;
import dev.wakey.audit.AuditLog;
import dev.wakey.tokens.TokenError;
import dev.wakey.tokens.TokenOwner;
import dev.wakey.tokens.TokenSummary;
import dev.wakey.web.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** The persistence adapter of the token aggregate: Spring Data JDBC writes and reads the rows. */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenStore {

    private final TokenRepository tokenRows;

    private final JdbcAggregateTemplate aggregates;

    private final AuditLog audit;

    private final TokenProperties properties;

    public void insert(TokenEntity token) {
        aggregates.insert(token);
    }

    public Optional<TokenEntity> findByHash(String tokenHash) {
        return tokenRows.findByTokenHash(tokenHash);
    }

    /** Rejects one more token of the owner when the space reached the configured owner limit. */
    public void requireCapacity(UUID spaceId, TokenOwner owner, Instant now) {
        int maxPerOwner = properties.maxPerOwner();
        long active = countActive(spaceId, owner, now);
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

    public long countActive(UUID spaceId, TokenOwner owner, Instant now) {
        Criteria expiresAfter = Criteria.where("expiresAt").greaterThan(now);
        Criteria active = Criteria.where("expiresAt").isNull().or(expiresAfter);
        Criteria criteria = ownerCriteria(spaceId, owner);
        criteria = criteria.and("revokedAt").isNull();
        criteria = criteria.and(active);
        return aggregates.count(Query.query(criteria), TokenEntity.class);
    }

    public List<TokenSummary> list(UUID spaceId, TokenOwner owner) {
        Criteria criteria = ownerCriteria(spaceId, owner);
        Sort newestFirst = Sort.by("createdAt", "id").descending();
        Query query = Query.query(criteria).sort(newestFirst);
        List<TokenEntity> found = aggregates.findAll(query, TokenEntity.class);
        return found.stream().map(TokenStore::toSummary).toList();
    }

    public void markUsed(UUID tokenId, Instant usedAt) {
        aggregates.update(new TokenUsage(tokenId, usedAt));
    }

    public boolean revoke(UUID spaceId, TokenOwner owner, UUID tokenId, Instant now) {
        Optional<TokenEntity> found = tokenRows.findById(tokenId);
        Optional<TokenEntity> revocable = found.filter(token -> token.spaceId().equals(spaceId))
                .filter(token -> token.ownerIssuer().equals(owner.issuer()))
                .filter(token -> token.ownerSubject().equals(owner.subject()))
                .filter(token -> token.revokedAt() == null);
        revocable.ifPresent(token -> revokeNow(token, now));
        if (revocable.isEmpty()) {
            return false;
        }
        log.info("Revoked token {} of user {} in space {}", tokenId, owner.subject(), spaceId);
        return true;
    }

    /**
     * Revokes every active token of one owner in one space; the member-removal listener uses it.
     * The active rows are read with one query and then updated in place, so a token keeps its id,
     * version and dependent rows; the owner's token limit bounds the number of updates.
     */
    @Transactional
    public int revokeAll(UUID spaceId, String ownerIssuer, String ownerSubject, Instant now) {
        Criteria criteria = Criteria.where("spaceId").is(spaceId);
        criteria = criteria.and("ownerIssuer").is(ownerIssuer);
        criteria = criteria.and("ownerSubject").is(ownerSubject);
        criteria = criteria.and("revokedAt").isNull();
        List<TokenEntity> active = aggregates.findAll(Query.query(criteria), TokenEntity.class);
        for (TokenEntity token : active) {
            revokeNow(token, now);
        }
        log.info("Revoked {} tokens of user {} in space {}", active.size(), ownerSubject, spaceId);
        recordRevokedAll(spaceId, ownerIssuer, ownerSubject, active.size());
        return active.size();
    }

    private void recordRevokedAll(UUID spaceId, String ownerIssuer, String ownerSubject, int revoked) {
        if (revoked == 0) {
            return;
        }
        Map<String, Object> details =
                Map.<String, Object>of("issuer", ownerIssuer, "subject", ownerSubject, "revoked", revoked);
        String objectId = spaceId.toString();
        AuditActor actor = new AuditActor(null, ActorKind.SYSTEM);
        AuditChange change = new AuditChange("token.revoke_all", "space_member", objectId, spaceId, actor, details);
        audit.record(change);
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
