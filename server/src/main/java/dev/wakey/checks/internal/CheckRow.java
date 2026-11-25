package dev.wakey.checks.internal;

import dev.wakey.checks.Check;
import dev.wakey.checks.CheckState;
import dev.wakey.checks.CheckType;
import dev.wakey.checks.PausedReason;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** One row of the check table together with its derived state; tags are loaded separately. */
record CheckRow(
        UUID id,
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
        CheckState state,
        long version,
        Instant createdAt,
        Instant updatedAt) {

    Check toCheck(List<String> tags) {
        return new Check(
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
                state,
                version,
                createdAt,
                updatedAt);
    }
}
