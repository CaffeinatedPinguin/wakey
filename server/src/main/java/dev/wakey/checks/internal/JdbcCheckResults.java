package dev.wakey.checks.internal;

import dev.wakey.checks.CheckResult;
import dev.wakey.checks.CheckResults;
import dev.wakey.checks.CheckState;
import dev.wakey.checks.ErrorCategory;
import dev.wakey.checks.InvalidCheckException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

/** PostgreSQL implementation of the append-only check results. */
final class JdbcCheckResults implements CheckResults {

    private static final String DEFAULT_LOCATION = "server";

    private static final String INSERT_SQL = """
            INSERT INTO check_result (id, check_id, location_id, checked_at, state, error_category,
                http_status, duration_ms, protocol_version, phases)
            VALUES (:id, :checkId, :locationId, :checkedAt, :state, :errorCategory, :httpStatus,
                :durationMs, :protocolVersion, :phases)
            """;

    private static final String LATEST_SQL = """
            SELECT location_id, checked_at, state, error_category, http_status, duration_ms,
                   protocol_version, phases
            FROM check_result
            WHERE check_id = :checkId
            ORDER BY %s
            LIMIT 1
            """.formatted(LatestResultOrder.NEWEST_FIRST);

    private static final String RECENT_SQL = """
            SELECT location_id, checked_at, state, error_category, http_status, duration_ms,
                   protocol_version, phases
            FROM check_result
            WHERE check_id = :checkId
            ORDER BY %s
            LIMIT :rowLimit
            """.formatted(LatestResultOrder.NEWEST_FIRST);

    private static final RowMapper<CheckResult> RESULT_MAPPER = (rs, unusedRowNum) -> {
        String locationId = rs.getString("location_id");
        Instant checkedAt = instant(rs);
        CheckState state = CheckState.valueOf(rs.getString("state"));
        @Nullable ErrorCategory errorCategory = errorCategory(rs.getString("error_category"));
        Integer httpStatus = rs.getObject("http_status", Integer.class);
        Integer durationMs = rs.getObject("duration_ms", Integer.class);
        String protocolVersion = rs.getString("protocol_version");
        String phases = rs.getString("phases");
        return new CheckResult(
                locationId, checkedAt, state, errorCategory, httpStatus, durationMs, protocolVersion, phases);
    };

    private final JdbcClient jdbc;

    JdbcCheckResults(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void record(UUID checkId, CheckResult result) {
        if (result.state() == CheckState.DOWN && result.errorCategory() == ErrorCategory.LOCATION_NETWORK) {
            throw new InvalidCheckException(
                    "state", "location_error_cannot_be_down", "A location network error cannot make the target down");
        }
        @Nullable String providedLocation = result.locationId();
        String locationId = providedLocation == null ? DEFAULT_LOCATION : providedLocation;
        String state = result.state().name();
        String errorCategory = nameOrNull(result.errorCategory());
        OffsetDateTime checkedAt = OffsetDateTime.ofInstant(result.checkedAt(), ZoneOffset.UTC);
        jdbc.sql(INSERT_SQL)
                .param("id", UUID.randomUUID())
                .param("checkId", checkId)
                .param("locationId", locationId)
                .param("checkedAt", checkedAt)
                .param("state", state)
                .param("errorCategory", errorCategory)
                .param("httpStatus", result.httpStatus())
                .param("durationMs", result.durationMs())
                .param("protocolVersion", result.protocolVersion())
                .param("phases", result.phases())
                .update();
    }

    @Override
    public Optional<CheckResult> latest(UUID checkId) {
        return jdbc.sql(LATEST_SQL)
                .param("checkId", checkId)
                .query(RESULT_MAPPER)
                .optional();
    }

    @Override
    public List<CheckResult> recent(UUID checkId, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return jdbc.sql(RECENT_SQL)
                .param("checkId", checkId)
                .param("rowLimit", limit)
                .query(RESULT_MAPPER)
                .list();
    }

    private static Instant instant(ResultSet rs) throws SQLException {
        return rs.getObject("checked_at", OffsetDateTime.class).toInstant();
    }

    private static @Nullable String nameOrNull(@Nullable Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static @Nullable ErrorCategory errorCategory(String value) {
        return value == null ? null : ErrorCategory.valueOf(value);
    }
}
