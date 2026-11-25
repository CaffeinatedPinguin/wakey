package dev.wakey.checks.internal;

import dev.wakey.checks.CheckCursor;
import dev.wakey.checks.CheckFilter;
import dev.wakey.checks.CheckState;
import dev.wakey.checks.CheckType;
import dev.wakey.checks.PausedReason;
import dev.wakey.checks.StateCounts;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Every SQL statement of the check module; callers pass the space id, so queries stay scoped. */
final class CheckSql {

    private static final String PLACEHOLDER = "{{latestState}}";

    private static final String LATEST_STATE = "COALESCE((SELECT r.state FROM check_result r WHERE r.check_id = c.id"
            + " ORDER BY r.checked_at DESC LIMIT 1), 'UNKNOWN')";

    private static final String FILTER_PREDICATES = """
            AND (CAST(:q AS VARCHAR) IS NULL
                 OR LOWER(c.name) LIKE LOWER(:q) ESCAPE '\\'
                 OR LOWER(c.target) LIKE LOWER(:q) ESCAPE '\\')
            AND (CAST(:state AS VARCHAR) IS NULL OR {{latestState}} = CAST(:state AS VARCHAR))
            AND (CAST(:type AS VARCHAR) IS NULL OR c.type = CAST(:type AS VARCHAR))
            AND (CAST(:tag AS VARCHAR) IS NULL
                 OR EXISTS (SELECT 1 FROM check_tag t
                            WHERE t.check_id = c.id AND t.tag = CAST(:tag AS VARCHAR)))
            AND (CAST(:groupName AS VARCHAR) IS NULL OR c.group_name = CAST(:groupName AS VARCHAR))
            AND (CAST(:pausedReason AS VARCHAR) IS NULL
                 OR c.paused_reason = CAST(:pausedReason AS VARCHAR))
            """;

    private static final String GET_SQL = ("""
            SELECT c.*, {{latestState}} AS state
            FROM "check" c
            WHERE c.space_id = :spaceId AND c.id = :id
            """).replace(PLACEHOLDER, LATEST_STATE);

    private static final String LIST_SQL = ("""
            SELECT c.*, {{latestState}} AS state
            FROM "check" c
            WHERE c.space_id = :spaceId
            """ + FILTER_PREDICATES + """
            AND (c.name, c.id) > (CAST(:afterName AS VARCHAR), CAST(:afterId AS UUID))
            ORDER BY c.name, c.id
            LIMIT :rowLimit
            """).replace(PLACEHOLDER, LATEST_STATE);

    // postgres-only: COUNT(*) FILTER computes the four state counters in one query; another database needs conditional
    // aggregates.
    private static final String COUNTS_SQL = ("""
            SELECT
              COUNT(*) FILTER (WHERE state = 'UP') AS up_count,
              COUNT(*) FILTER (WHERE state = 'DEGRADED') AS degraded_count,
              COUNT(*) FILTER (WHERE state = 'DOWN') AS down_count,
              COUNT(*) FILTER (WHERE state = 'UNKNOWN') AS unknown_count
            FROM (
              SELECT {{latestState}} AS state
              FROM "check" c
              WHERE c.space_id = :spaceId
              """ + FILTER_PREDICATES + """
            ) counted
            """).replace(PLACEHOLDER, LATEST_STATE);

    private static final String INSERT_CHECK_SQL = """
            INSERT INTO "check" (id, space_id, name, type, target, interval_seconds, timeout_seconds,
                retries, group_name, description, runbook_url, paused, paused_reason, version,
                created_at, updated_at)
            VALUES (:id, :spaceId, :name, :type, :target, :intervalSeconds, :timeoutSeconds,
                :retries, :groupName, :description, :runbookUrl, FALSE, NULL, 1,
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

    private static final String UPDATE_CHECK_SQL = """
            UPDATE "check"
            SET name = :name,
                type = :type,
                target = :target,
                interval_seconds = :intervalSeconds,
                timeout_seconds = :timeoutSeconds,
                retries = :retries,
                group_name = :groupName,
                description = :description,
                runbook_url = :runbookUrl,
                version = version + 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE space_id = :spaceId AND id = :id AND version = :expectedVersion
            """;

    private static final String PAUSE_SQL = """
            UPDATE "check"
            SET paused = TRUE, paused_reason = :pausedReason, version = version + 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE space_id = :spaceId AND id = :id
            """;

    private static final String RESUME_SQL = """
            UPDATE "check"
            SET paused = FALSE, paused_reason = NULL, version = version + 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE space_id = :spaceId AND id = :id
            """;

    private static final String DELETE_SQL = """
            DELETE FROM "check"
            WHERE space_id = :spaceId AND id = :id
            """;

    private static final String SELECT_TAGS_SQL = """
            SELECT tag
            FROM check_tag
            WHERE check_id = :checkId
            ORDER BY tag
            """;

    private static final String INSERT_TAG_SQL = """
            INSERT INTO check_tag (check_id, tag)
            VALUES (:checkId, :tag)
            """;

    private static final String DELETE_TAGS_SQL = """
            DELETE FROM check_tag
            WHERE check_id = :checkId
            """;

    private static final RowMapper<CheckRow> ROW_MAPPER = CheckSql::mapRow;

    private final JdbcClient jdbc;

    CheckSql(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    Optional<CheckRow> find(UUID spaceId, UUID id) {
        return jdbc.sql(GET_SQL)
                .param("spaceId", spaceId)
                .param("id", id)
                .query(ROW_MAPPER)
                .optional();
    }

    List<CheckRow> list(UUID spaceId, CheckFilter filter, CheckCursor cursor, int limit) {
        JdbcClient.StatementSpec spec = bindFilter(jdbc.sql(LIST_SQL), spaceId, filter);
        return spec.param("afterName", cursor.name())
                .param("afterId", cursor.id())
                .param("rowLimit", limit + 1)
                .query(ROW_MAPPER)
                .list();
    }

    StateCounts counts(UUID spaceId, CheckFilter filter) {
        JdbcClient.StatementSpec spec = bindFilter(jdbc.sql(COUNTS_SQL), spaceId, filter);
        return spec.query((rs, unusedRowNum) -> new StateCounts(
                        rs.getLong("up_count"),
                        rs.getLong("degraded_count"),
                        rs.getLong("down_count"),
                        rs.getLong("unknown_count")))
                .single();
    }

    void insert(UUID spaceId, UUID id, ValidatedCheck check) {
        String type = check.type().name();
        jdbc.sql(INSERT_CHECK_SQL)
                .param("id", id)
                .param("spaceId", spaceId)
                .param("name", check.name())
                .param("type", type)
                .param("target", check.target())
                .param("intervalSeconds", check.intervalSeconds())
                .param("timeoutSeconds", check.timeoutSeconds())
                .param("retries", check.retries())
                .param("groupName", check.groupName())
                .param("description", check.description())
                .param("runbookUrl", check.runbookUrl())
                .update();
        insertTags(id, check.tags());
    }

    int update(UUID spaceId, UUID id, ValidatedCheck check, long expectedVersion) {
        String type = check.type().name();
        int updated = jdbc.sql(UPDATE_CHECK_SQL)
                .param("name", check.name())
                .param("type", type)
                .param("target", check.target())
                .param("intervalSeconds", check.intervalSeconds())
                .param("timeoutSeconds", check.timeoutSeconds())
                .param("retries", check.retries())
                .param("groupName", check.groupName())
                .param("description", check.description())
                .param("runbookUrl", check.runbookUrl())
                .param("spaceId", spaceId)
                .param("id", id)
                .param("expectedVersion", expectedVersion)
                .update();
        if (updated == 1) {
            replaceTags(id, check.tags());
        }
        return updated;
    }

    int pause(UUID spaceId, UUID id, PausedReason reason) {
        return jdbc.sql(PAUSE_SQL)
                .param("pausedReason", reason.name())
                .param("spaceId", spaceId)
                .param("id", id)
                .update();
    }

    int resume(UUID spaceId, UUID id) {
        return jdbc.sql(RESUME_SQL).param("spaceId", spaceId).param("id", id).update();
    }

    int delete(UUID spaceId, UUID id) {
        return jdbc.sql(DELETE_SQL).param("spaceId", spaceId).param("id", id).update();
    }

    List<String> tags(UUID checkId) {
        return jdbc.sql(SELECT_TAGS_SQL)
                .param("checkId", checkId)
                .query((rs, unusedRowNum) -> rs.getString("tag"))
                .list();
    }

    private void replaceTags(UUID checkId, List<String> tags) {
        jdbc.sql(DELETE_TAGS_SQL).param("checkId", checkId).update();
        insertTags(checkId, tags);
    }

    private void insertTags(UUID checkId, List<String> tags) {
        for (String tag : tags) {
            jdbc.sql(INSERT_TAG_SQL).param("checkId", checkId).param("tag", tag).update();
        }
    }

    private static JdbcClient.StatementSpec bindFilter(
            JdbcClient.StatementSpec spec, UUID spaceId, CheckFilter filter) {
        @Nullable String q = likePattern(filter.q());
        @Nullable String state = nameOrNull(filter.state());
        @Nullable String type = nameOrNull(filter.type());
        @Nullable String tag = filter.tag();
        @Nullable String groupName = filter.group();
        @Nullable String pausedReason = nameOrNull(filter.pausedReason());
        return spec.param("spaceId", spaceId)
                .param("q", q)
                .param("state", state)
                .param("type", type)
                .param("tag", tag)
                .param("groupName", groupName)
                .param("pausedReason", pausedReason);
    }

    private static CheckRow mapRow(ResultSet rs, int unusedRowNum) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        UUID spaceId = rs.getObject("space_id", UUID.class);
        String name = rs.getString("name");
        CheckType type = CheckType.valueOf(rs.getString("type"));
        String target = rs.getString("target");
        int intervalSeconds = rs.getInt("interval_seconds");
        int timeoutSeconds = rs.getInt("timeout_seconds");
        int retries = rs.getInt("retries");
        String groupName = rs.getString("group_name");
        String description = rs.getString("description");
        String runbookUrl = rs.getString("runbook_url");
        boolean paused = rs.getBoolean("paused");
        @Nullable PausedReason pausedReason = pausedReason(rs.getString("paused_reason"));
        CheckState state = CheckState.valueOf(rs.getString("state"));
        long version = rs.getLong("version");
        Instant createdAt = instant(rs, "created_at");
        Instant updatedAt = instant(rs, "updated_at");
        return new CheckRow(
                id,
                spaceId,
                name,
                type,
                target,
                intervalSeconds,
                timeoutSeconds,
                retries,
                groupName,
                description,
                runbookUrl,
                paused,
                pausedReason,
                state,
                version,
                createdAt,
                updatedAt);
    }

    private static @Nullable String likePattern(@Nullable String q) {
        @Nullable String trimmed = q == null ? null : q.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            return null;
        }
        return "%" + escapeLike(trimmed) + "%";
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static @Nullable String nameOrNull(@Nullable Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, OffsetDateTime.class).toInstant();
    }

    private static @Nullable PausedReason pausedReason(String value) {
        return value == null ? null : PausedReason.valueOf(value);
    }
}
