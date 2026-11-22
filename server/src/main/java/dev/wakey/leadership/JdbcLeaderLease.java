package dev.wakey.leadership;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

@Slf4j
@RequiredArgsConstructor
final class JdbcLeaderLease implements LeaderLease {

    private static final RowMapper<LeaseGrant> GRANT_MAPPER = (rs, unusedRowNum) -> new LeaseGrant(
            rs.getString("name"),
            rs.getString("holder_id"),
            rs.getLong("fencing_token"),
            rs.getObject("expires_at", OffsetDateTime.class));

    private static final String RENEW_SQL = """
            UPDATE leader_lock
            SET expires_at = CURRENT_TIMESTAMP + CAST(:ttlSeconds AS DOUBLE PRECISION) * INTERVAL '1' SECOND
            WHERE name = :name AND holder_id = :holderId
            """;

    private static final String TAKE_OVER_SQL = """
            UPDATE leader_lock
            SET holder_id = :holderId,
                fencing_token = fencing_token + 1,
                expires_at = CURRENT_TIMESTAMP + CAST(:ttlSeconds AS DOUBLE PRECISION) * INTERVAL '1' SECOND
            WHERE name = :name AND expires_at <= CURRENT_TIMESTAMP
            """;

    private static final String INSERT_SQL = """
            INSERT INTO leader_lock (name, holder_id, fencing_token, expires_at)
            VALUES (:name, :holderId, 1, CURRENT_TIMESTAMP + CAST(:ttlSeconds AS DOUBLE PRECISION) * INTERVAL '1' SECOND)
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

    private final Duration ttl;

    @Override
    public Optional<LeaseGrant> acquireOrRenew(String name) {
        log.debug("Trying to acquire or renew lease {}", name);
        Optional<LeaseGrant> grant;
        if (renew(name)) {
            grant = read(name);
            grant.ifPresent(this::logRenewal);
        } else if (takeOverExpired(name)) {
            grant = readNewGrant(name, "Took over expired lease");
        } else if (insert(name)) {
            grant = readNewGrant(name, "Created lease");
        } else {
            log.debug("Lease {} is held by another holder; {} stays in reserve", name, holderId);
            grant = Optional.empty();
        }
        return grant;
    }

    @Override
    public void release(String name) {
        int updated = jdbc.sql(RELEASE_SQL)
                .param("name", name)
                .param("holderId", holderId)
                .update();
        if (updated == 1) {
            log.info("Released lease {} held by {}", name, holderId);
        } else {
            log.debug("Lease {} is not held by {}; nothing to release", name, holderId);
        }
    }

    private boolean renew(String name) {
        int updated = jdbc.sql(RENEW_SQL)
                .param("name", name)
                .param("holderId", holderId)
                .param("ttlSeconds", ttlSeconds())
                .update();
        return updated == 1;
    }

    private boolean takeOverExpired(String name) {
        int updated = jdbc.sql(TAKE_OVER_SQL)
                .param("name", name)
                .param("holderId", holderId)
                .param("ttlSeconds", ttlSeconds())
                .update();
        return updated == 1;
    }

    private boolean insert(String name) {
        try {
            int inserted = jdbc.sql(INSERT_SQL)
                    .param("name", name)
                    .param("holderId", holderId)
                    .param("ttlSeconds", ttlSeconds())
                    .update();
            return inserted == 1;
        } catch (DuplicateKeyException exception) {
            log.debug("Lease {} was created concurrently by another holder", name, exception);
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

    private Optional<LeaseGrant> readNewGrant(String name, String event) {
        Optional<LeaseGrant> grant = read(name);
        grant.ifPresent(value -> log.info(
                "{} {} for holder {} with fencing token {} until {}",
                event,
                value.name(),
                value.holderId(),
                value.fencingToken(),
                value.expiresAt()));
        return grant;
    }

    private void logRenewal(LeaseGrant grant) {
        log.debug(
                "Renewed lease {} for holder {} with fencing token {} until {}",
                grant.name(),
                grant.holderId(),
                grant.fencingToken(),
                grant.expiresAt());
    }

    private long ttlSeconds() {
        return ttl.toSeconds();
    }
}
