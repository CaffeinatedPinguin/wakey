package dev.wakey.tokens.internal;

import dev.wakey.tokens.TokenScope;
import dev.wakey.tokens.TokenSummary;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

/** All token SQL lives here: the hash is the only secret-like value the database sees. */
@RequiredArgsConstructor
final class ApiTokenStore {

    private static final String INSERT_SQL = """
            INSERT INTO api_token (id, space_id, owner_subject, name, scope, token_hash,
                                   expires_at, created_at, last_used_at, revoked_at)
            VALUES (:id, :spaceId, :ownerSubject, :name, :scope, :tokenHash,
                    :expiresAt, :createdAt, NULL, NULL)
            """;

    private static final String FIND_BY_HASH_SQL = """
            SELECT t.id, t.space_id, t.owner_subject, t.scope, t.expires_at, t.revoked_at, s.slug
            FROM api_token t
            JOIN space s ON s.id = t.space_id
            WHERE t.token_hash = :tokenHash
            """;

    private static final String MARK_USED_SQL = """
            UPDATE api_token
            SET last_used_at = :lastUsedAt
            WHERE id = :id
            """;

    private static final String REVOKE_SQL = """
            UPDATE api_token
            SET revoked_at = :revokedAt
            WHERE id = :id AND space_id = :spaceId AND owner_subject = :ownerSubject AND revoked_at IS NULL
            """;

    private static final String LIST_SQL = """
            SELECT id, name, scope, expires_at, created_at, last_used_at, revoked_at
            FROM api_token
            WHERE space_id = :spaceId AND owner_subject = :ownerSubject
            ORDER BY created_at, id
            """;

    private static final RowMapper<StoredToken> STORED_TOKEN_MAPPER = (rs, unusedRowNum) -> new StoredToken(
            rs.getObject("id", UUID.class),
            rs.getObject("space_id", UUID.class),
            rs.getString("owner_subject"),
            tokenScope(rs),
            toInstant(rs, "expires_at"),
            toInstant(rs, "revoked_at"),
            rs.getString("slug"));

    private static final RowMapper<TokenSummary> SUMMARY_MAPPER = (rs, unusedRowNum) -> new TokenSummary(
            rs.getObject("id", UUID.class),
            rs.getString("name"),
            tokenScope(rs),
            requiredInstant(rs, "created_at"),
            toInstant(rs, "expires_at"),
            toInstant(rs, "last_used_at"),
            toInstant(rs, "revoked_at"));

    private final JdbcClient jdbc;

    void insert(NewToken token) {
        String scope = token.scope().name();
        OffsetDateTime expiresAt = toDatabaseNullable(token.expiresAt());
        OffsetDateTime createdAt = toDatabase(token.createdAt());
        jdbc.sql(INSERT_SQL)
                .param("id", token.id())
                .param("spaceId", token.spaceId())
                .param("ownerSubject", token.ownerSubject())
                .param("name", token.name())
                .param("scope", scope)
                .param("tokenHash", token.tokenHash())
                .param("expiresAt", expiresAt)
                .param("createdAt", createdAt)
                .update();
    }

    Optional<StoredToken> findByHash(String tokenHash) {
        return jdbc.sql(FIND_BY_HASH_SQL)
                .param("tokenHash", tokenHash)
                .query(STORED_TOKEN_MAPPER)
                .optional();
    }

    void markUsed(UUID tokenId, Instant usedAt) {
        OffsetDateTime lastUsedAt = toDatabase(usedAt);
        jdbc.sql(MARK_USED_SQL)
                .param("lastUsedAt", lastUsedAt)
                .param("id", tokenId)
                .update();
    }

    void revoke(UUID spaceId, String ownerSubject, UUID tokenId, Instant revokedAt) {
        OffsetDateTime value = toDatabase(revokedAt);
        jdbc.sql(REVOKE_SQL)
                .param("revokedAt", value)
                .param("id", tokenId)
                .param("spaceId", spaceId)
                .param("ownerSubject", ownerSubject)
                .update();
    }

    List<TokenSummary> list(UUID spaceId, String ownerSubject) {
        return jdbc.sql(LIST_SQL)
                .param("spaceId", spaceId)
                .param("ownerSubject", ownerSubject)
                .query(SUMMARY_MAPPER)
                .list();
    }

    private static TokenScope tokenScope(ResultSet rs) throws SQLException {
        return TokenScope.valueOf(rs.getString("scope"));
    }

    private static OffsetDateTime toDatabase(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static @Nullable OffsetDateTime toDatabaseNullable(@Nullable Instant instant) {
        return instant == null ? null : toDatabase(instant);
    }

    private static @Nullable Instant toInstant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private static Instant requiredInstant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return Objects.requireNonNull(value).toInstant();
    }
}
