package dev.wakey.checks.internal;

import dev.wakey.checks.CheckState;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** One interval of a stable check state; an open interval has a null {@code endedAt}. */
@Table("check_state_interval")
public record CheckStateIntervalEntity(
        @Id UUID id,
        UUID checkId,
        UUID spaceId,
        CheckState state,
        Instant startedAt,
        @Nullable Instant endedAt) {

    static CheckStateIntervalEntity open(UUID checkId, UUID spaceId, CheckState state, Instant startedAt) {
        return new CheckStateIntervalEntity(UUID.randomUUID(), checkId, spaceId, state, startedAt, null);
    }

    CheckStateIntervalEntity endedAt(Instant ended) {
        return new CheckStateIntervalEntity(id, checkId, spaceId, state, startedAt, ended);
    }
}
