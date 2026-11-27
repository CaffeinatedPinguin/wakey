package dev.wakey.checks.internal;

import dev.wakey.checks.Check;
import dev.wakey.checks.CheckDraft;
import dev.wakey.checks.CheckState;
import dev.wakey.checks.CheckType;
import dev.wakey.checks.PausedReason;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/** A check row; the state of its newest result is stored on the row, never derived per list item. */
@Table("check")
public record CheckEntity(
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
        @MappedCollection(idColumn = "check_id") Set<CheckTag> tags,
        CheckState lastState,
        @Version long version,
        Instant createdAt,
        Instant updatedAt) {

    static CheckEntity created(UUID id, UUID spaceId, CheckDraft draft, Instant now) {
        Set<CheckTag> tags = tagSet(spaceId, draft.tags());
        return new CheckEntity(
                id,
                spaceId,
                draft.name(),
                draft.type(),
                draft.target(),
                draft.intervalSeconds(),
                draft.timeoutSeconds(),
                draft.retries(),
                draft.groupName(),
                draft.description(),
                draft.runbookUrl(),
                false,
                null,
                tags,
                CheckState.UNKNOWN,
                0L,
                now,
                now);
    }

    static CheckEntity updated(Check existing, CheckDraft changes, long expectedVersion, Instant now) {
        Set<CheckTag> tags = tagSet(existing.spaceId(), changes.tags());
        return new CheckEntity(
                existing.id(),
                existing.spaceId(),
                changes.name(),
                changes.type(),
                changes.target(),
                changes.intervalSeconds(),
                changes.timeoutSeconds(),
                changes.retries(),
                changes.groupName(),
                changes.description(),
                changes.runbookUrl(),
                existing.paused(),
                existing.pausedReason(),
                tags,
                existing.state(),
                expectedVersion,
                existing.createdAt(),
                now);
    }

    CheckEntity paused(PausedReason reason, Instant now) {
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
                true,
                reason,
                tags,
                lastState,
                version,
                createdAt,
                now);
    }

    CheckEntity resumed(Instant now) {
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
                false,
                null,
                tags,
                lastState,
                version,
                createdAt,
                now);
    }

    private static Set<CheckTag> tagSet(UUID spaceId, List<String> tags) {
        Set<CheckTag> entities = new LinkedHashSet<>();
        for (String tag : tags) {
            entities.add(new CheckTag(spaceId, tag));
        }
        return entities;
    }
}
