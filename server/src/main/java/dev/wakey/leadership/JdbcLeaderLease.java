package dev.wakey.leadership;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

final class JdbcLeaderLease implements LeaderLease {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcLeaderLease.class);

    private static final RowMapper<LeaseGrant> GRANT_MAPPER = (rs, unusedRowNum) -> new LeaseGrant(
            rs.getString("name"),
            rs.getString("holder_id"),
            rs.getLong("fencing_token"),
            rs.getObject("expires_at", OffsetDateTime.class));

    private static final String CLAIM_SQL = """
            UPDATE leader_lock
            SET holder_id = :holderId,
                fencing_token = CASE WHEN holder_id = :holderId THEN fencing_token ELSE fencing_token + 1 END,
                expires_at = CURRENT_TIMESTAMP + INTERVAL '%s' SECOND
            WHERE name = :name
              AND (holder_id = :holderId OR expires_at <= CURRENT_TIMESTAMP)
            """;

    private static final String INSERT_SQL = """
            INSERT INTO leader_lock (name, holder_id, fencing_token, expires_at)
            VALUES (:name, :holderId, 1, CURRENT_TIMESTAMP + INTERVAL '%s' SECOND)
            """;

    private static final String RELEASE_SQL = """
            UPDATE leader_lock
            SET expires_at = CURRENT_TIMESTAMP
            WHERE name = :name AND holder_id = :holderId
            """;

    private static final String SELECT_GRANT_SQL = """
            SELECT name, holder_id, fencing_token, expires_at
            FROM leader_lock
            WHERE name = :name AND holder_id = :holderId
            """;

    private final JdbcClient jdbc;

    private final String holderId;

    private final String claimSql;

    private final String insertSql;

    JdbcLeaderLease(JdbcClient jdbc, String holderId, Duration ttl) {
        this.jdbc = jdbc;
        this.holderId = holderId;
        String interval = intervalLiteral(ttl);
        this.claimSql = CLAIM_SQL.formatted(interval);
        this.insertSql = INSERT_SQL.formatted(interval);
    }

    @Override
    public Optional<LeaseGrant> acquireOrRenew(String name) {
        Optional<LeaseGrant> grant = Optional.empty();
        if (claim(name) || insert(name)) {
            grant = read(name);
        }
        return grant;
    }

    @Override
    public void release(String name) {
        jdbc.sql(RELEASE_SQL).param("name", name).param("holderId", holderId).update();
    }

    private boolean claim(String name) {
        int updated = jdbc.sql(claimSql)
                .param("name", name)
                .param("holderId", holderId)
                .update();
        return updated == 1;
    }

    private boolean insert(String name) {
        try {
            int inserted = jdbc.sql(insertSql)
                    .param("name", name)
                    .param("holderId", holderId)
                    .update();
            return inserted == 1;
        } catch (DuplicateKeyException exception) {
            LOG.debug("Lease {} was created concurrently by another holder", name, exception);
            return false;
        }
    }

    private Optional<LeaseGrant> read(String name) {
        return jdbc.sql(SELECT_GRANT_SQL)
                .param("name", name)
                .param("holderId", holderId)
                .query(GRANT_MAPPER)
                .optional();
    }

    private static String intervalLiteral(Duration ttl) {
        return String.format(Locale.ROOT, "%d.%06d", ttl.getSeconds(), ttl.getNano() / 1_000);
    }
}
