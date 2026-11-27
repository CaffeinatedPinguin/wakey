package dev.wakey.checks.internal;

import dev.wakey.checks.CheckState;
import dev.wakey.checks.CheckType;
import dev.wakey.checks.PausedReason;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** A check row without its tags; the list reads the tags of a whole page with one derived query. */
@Table("check")
public record CheckRow(
        @Id UUID id,
        UUID spaceId,
        String name,
        CheckType type,
        String target,
        int intervalSeconds,
        int timeoutSeconds,
        int retries,
        @Nullable String groupName,
        @Nullable String description,
        @Nullable String runbookUrl,
        boolean paused,
        @Nullable PausedReason pausedReason,
        CheckState lastState,
        long version,
        Instant createdAt,
        Instant updatedAt) {

    CheckEntity toEntity(Set<CheckTag> tags) {
        return new CheckEntity(
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
                tags,
                lastState,
                version,
                createdAt,
                updatedAt);
    }
}
