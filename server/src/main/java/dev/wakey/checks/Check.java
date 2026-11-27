package dev.wakey.checks;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A check as stored together with its derived current state and tags. The state comes from the
 * latest result; a check without results is {@link CheckState#UNKNOWN}.
 */
public record Check(
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
        List<String> tags,
        CheckState state,
        long version,
        Instant createdAt,
        Instant updatedAt) {}
