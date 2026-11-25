package dev.wakey.checks.internal;

import dev.wakey.checks.Check;
import dev.wakey.checks.CheckChanges;
import dev.wakey.checks.CheckCursor;
import dev.wakey.checks.CheckFilter;
import dev.wakey.checks.CheckNotFoundException;
import dev.wakey.checks.CheckPage;
import dev.wakey.checks.CheckService;
import dev.wakey.checks.CheckVersionConflictException;
import dev.wakey.checks.NewCheck;
import dev.wakey.checks.PausedReason;
import dev.wakey.checks.StateCounts;
import java.util.List;
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
    public Check create(UUID spaceId, NewCheck check) {
        ValidatedCheck validated = CheckValidator.validate(check);
        UUID id = UUID.randomUUID();
        sql.insert(spaceId, id, validated);
        return get(spaceId, id);
    }

    @Override
    @Transactional
    public Check update(UUID spaceId, UUID id, CheckChanges changes, long expectedVersion) {
        CheckRow existing = findRequiredRow(spaceId, id);
        if (existing.version() != expectedVersion) {
            throw new CheckVersionConflictException(id, expectedVersion, existing.version());
        }
        ValidatedCheck validated = CheckValidator.validate(changes);
        int updated = sql.update(spaceId, id, validated, expectedVersion);
        if (updated == 0) {
            throw new CheckVersionConflictException(id, expectedVersion, existing.version());
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
            throw new CheckNotFoundException(id);
        }
    }

    @Override
    @Transactional
    public void resume(UUID spaceId, UUID id) {
        int updated = sql.resume(spaceId, id);
        if (updated == 0) {
            throw new CheckNotFoundException(id);
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
        List<Check> items = pageRows.stream().map(this::toCheck).toList();
        @Nullable CheckCursor next = hasMore ? cursorOf(pageRows.getLast()) : null;
        return new CheckPage(List.copyOf(items), next);
    }

    private static CheckCursor cursorOf(CheckRow row) {
        return new CheckCursor(row.name(), row.id());
    }
}
