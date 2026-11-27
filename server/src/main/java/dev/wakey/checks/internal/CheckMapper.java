package dev.wakey.checks.internal;

import dev.wakey.checks.Check;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Maps stored check rows to the public records of the module. */
final class CheckMapper {

    private CheckMapper() {}

    static Check toCheck(CheckEntity entity) {
        List<String> tags = tagTexts(entity.tags());
        return new Check(
                entity.id(),
                entity.spaceId(),
                entity.name(),
                entity.type(),
                entity.target(),
                entity.intervalSeconds(),
                entity.timeoutSeconds(),
                entity.retries(),
                entity.groupName(),
                entity.description(),
                entity.runbookUrl(),
                entity.paused(),
                entity.pausedReason(),
                tags,
                entity.lastState(),
                entity.version(),
                entity.createdAt(),
                entity.updatedAt());
    }

    static List<Check> toChecks(List<CheckRow> rows, List<CheckTagRow> tagRows) {
        List<Check> items = new ArrayList<>(rows.size());
        for (CheckEntity entity : toEntities(rows, tagRows)) {
            items.add(toCheck(entity));
        }
        return List.copyOf(items);
    }

    static List<CheckEntity> toEntities(List<CheckRow> rows, List<CheckTagRow> tagRows) {
        Map<UUID, Set<CheckTag>> tagsByCheck = tagSets(tagRows);
        List<CheckEntity> entities = new ArrayList<>(rows.size());
        for (CheckRow row : rows) {
            UUID id = row.id();
            Set<CheckTag> tags = tagsByCheck.getOrDefault(id, Set.of());
            entities.add(row.toEntity(tags));
        }
        return List.copyOf(entities);
    }

    private static Map<UUID, Set<CheckTag>> tagSets(List<CheckTagRow> tagRows) {
        Map<UUID, Set<CheckTag>> tagsByCheck = new HashMap<>();
        for (CheckTagRow row : tagRows) {
            CheckTagKey key = row.key();
            UUID spaceId = row.spaceId();
            String tag = key.tag();
            tagsByCheck
                    .computeIfAbsent(key.checkId(), _ -> new LinkedHashSet<>())
                    .add(new CheckTag(spaceId, tag));
        }
        return tagsByCheck;
    }

    private static List<String> tagTexts(Set<CheckTag> tags) {
        List<String> values = new ArrayList<>(tags.size());
        for (CheckTag tag : tags) {
            values.add(tag.tag());
        }
        values.sort(Comparator.naturalOrder());
        return List.copyOf(values);
    }
}
