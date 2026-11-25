package dev.wakey.spaces.internal;

import dev.wakey.spaces.Space;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceView;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.RowMapper;

/** Row mappers for the spaces module; the membership start time is read next to the role. */
final class SpaceRowMappers {

    static final RowMapper<Space> SPACE = (rs, unusedRowNum) -> new Space(
            rs.getObject("id", UUID.class),
            rs.getString("slug"),
            rs.getString("name"),
            rs.getString("plan_name"),
            rs.getLong("version"));

    private SpaceRowMappers() {}

    static RowMapper<SpaceView> view(boolean admin) {
        return (rs, unusedRowNum) -> {
            String roleValue = rs.getString("role");
            @Nullable SpaceRole role = roleOrNull(roleValue);
            return new SpaceView(
                    rs.getObject("id", UUID.class),
                    rs.getString("slug"),
                    rs.getString("name"),
                    rs.getString("plan_name"),
                    rs.getLong("version"),
                    admin,
                    role,
                    instantOrNull(rs, "since"));
        };
    }

    private static @Nullable Instant instantOrNull(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private static @Nullable SpaceRole roleOrNull(String value) {
        return value == null ? null : SpaceRole.valueOf(value);
    }
}
