package dev.wakey.checks.internal;

import dev.wakey.checks.Check;
import dev.wakey.checks.CheckCursor;
import dev.wakey.checks.CheckDraft;
import dev.wakey.checks.CheckFilter;
import dev.wakey.checks.CheckNotFoundException;
import dev.wakey.checks.CheckPage;
import dev.wakey.checks.CheckPlanLimitException;
import dev.wakey.checks.CheckService;
import dev.wakey.checks.CheckUpdate;
import dev.wakey.checks.CheckVersionConflictException;
import dev.wakey.checks.PausedReason;
import dev.wakey.checks.PlanLimits;
import dev.wakey.checks.StateCounts;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

/**
 * PostgreSQL implementation of the check port; every query is scoped by the space id. The class is
 * not final because Spring creates a transactional proxy around the bean.
 */
class JdbcCheckService implements CheckService {

    private static final CheckCursor FIRST_CURSOR =
            new CheckCursor("", UUID.fromString("00000000-0000-0000-0000-000000000000"));

    private final CheckSql sql;

    JdbcCheckService(CheckSql sql) {
        this.sql = sql;
    }

    @Override
    @Transactional
    public Check create(UUID spaceId, CheckDraft check, PlanLimits limits) {
        CheckDraft validated = CheckValidator.validate(check, limits);
        sql.lockSpace(spaceId);
        requireCheckCapacity(spaceId, limits);
        UUID id = UUID.randomUUID();
        sql.insert(spaceId, id, validated);
        return get(spaceId, id);
    }

    @Override
    @Transactional
    public Check update(UUID spaceId, CheckUpdate update) {
        UUID id = update.id();
        CheckRow existing = findRequiredRow(spaceId, id);
        long expectedVersion = update.expectedVersion();
        long currentVersion = existing.version();
        if (currentVersion != expectedVersion) {
            throw new CheckVersionConflictException(id, expectedVersion, currentVersion);
        }
        CheckDraft changes = update.changes();
        PlanLimits limits = update.limits();
        CheckDraft validated = CheckValidator.validate(changes, limits);
        int updated = sql.update(spaceId, id, validated, expectedVersion);
        if (updated == 0) {
            throw new CheckVersionConflictException(id, expectedVersion, currentVersion);
        }
        return get(spaceId, id);
    }

    @Override
    public Check get(UUID spaceId, UUID id) {
        return toCheck(findRequiredRow(spaceId, id));
    }

    @Override
    public CheckPage list(UUID spaceId, CheckFilter filter, @Nullable CheckCursor after, int limit) {
        if (limit <= 0) {
            return new CheckPage(List.of(), null);
        }
        CheckCursor cursor = after == null ? FIRST_CURSOR : after;
        List<CheckRow> rows = sql.list(spaceId, filter, cursor, limit);
        return pageOf(rows, limit);
    }

    @Override
    public StateCounts counts(UUID spaceId, CheckFilter filter) {
        return sql.counts(spaceId, filter);
    }

    @Override
    @Transactional
    public void pause(UUID spaceId, UUID id, PausedReason reason) {
        int updated = sql.pause(spaceId, id, reason);
        if (updated == 0) {
            findRequiredRow(spaceId, id);
        }
    }

    @Override
    @Transactional
    public void resume(UUID spaceId, UUID id) {
        int updated = sql.resume(spaceId, id);
        if (updated == 0) {
            findRequiredRow(spaceId, id);
        }
    }

    @Override
    @Transactional
    public void delete(UUID spaceId, UUID id) {
        int deleted = sql.delete(spaceId, id);
        if (deleted == 0) {
            throw new CheckNotFoundException(id);
        }
    }

    private void requireCheckCapacity(UUID spaceId, PlanLimits limits) {
        Integer maxChecks = limits.maxChecks();
        if (maxChecks == null) {
            return;
        }
        long count = sql.count(spaceId);
        if (count >= maxChecks) {
            throw new CheckPlanLimitException(maxChecks, limits.planName());
        }
    }

    private CheckRow findRequiredRow(UUID spaceId, UUID id) {
        return sql.find(spaceId, id).orElseThrow(() -> new CheckNotFoundException(id));
    }

    private Check toCheck(CheckRow row) {
        UUID id = row.id();
        List<String> tags = sql.tags(id);
        return row.toCheck(tags);
    }

    private CheckPage pageOf(List<CheckRow> rows, int limit) {
        boolean hasMore = rows.size() > limit;
        List<CheckRow> pageRows = hasMore ? rows.subList(0, limit) : rows;
        List<UUID> ids = pageRows.stream().map(CheckRow::id).toList();
        Map<UUID, List<String>> tags = sql.tagsFor(ids);
        List<Check> items =
                pageRows.stream().map(row -> toCheckWithTags(row, tags)).toList();
        @Nullable CheckCursor next = hasMore ? cursorOf(pageRows.getLast()) : null;
        return new CheckPage(List.copyOf(items), next);
    }

    private static Check toCheckWithTags(CheckRow row, Map<UUID, List<String>> tags) {
        List<String> rowTags = tags.getOrDefault(row.id(), List.of());
        return row.toCheck(rowTags);
    }

    private static CheckCursor cursorOf(CheckRow row) {
        return new CheckCursor(row.name(), row.id());
    }
}
