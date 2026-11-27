package dev.wakey.checks.internal;

import dev.wakey.checks.Check;
import dev.wakey.checks.CheckDraft;
import dev.wakey.checks.CheckFilter;
import dev.wakey.checks.CheckPosition;
import dev.wakey.checks.CheckResult;
import dev.wakey.checks.PausedReason;
import dev.wakey.checks.StateCounts;
import dev.wakey.web.ApiException;
import dev.wakey.web.CommonError;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/** The persistence adapter of the check aggregate: Spring Data JDBC repositories and criteria. */
@Component
@RequiredArgsConstructor
public class CheckStore {

    private final CheckRepository checks;

    private final CheckQueries queries;

    private final CheckResultStore results;

    private final Clock clock;

    public Check get(UUID spaceId, UUID id) {
        return CheckMapper.toCheck(requiredEntity(spaceId, id));
    }

    public List<Check> list(UUID spaceId, CheckFilter filter, @Nullable CheckPosition after, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return queries.list(spaceId, filter, after, limit);
    }

    public StateCounts counts(UUID spaceId, CheckFilter filter) {
        return queries.counts(spaceId, filter);
    }

    public Optional<CheckResult> latest(UUID checkId) {
        return results.latest(checkId);
    }

    public List<CheckEntity> targets(UUID spaceId, @Nullable List<UUID> ids, @Nullable CheckFilter filter) {
        if (ids != null) {
            List<UUID> distinct = ids.stream().distinct().toList();
            return queries.entities(spaceId, distinct);
        }
        return queries.entities(spaceId, filter == null ? CheckFilter.all() : filter);
    }

    public long count(UUID spaceId) {
        return checks.countBySpaceId(spaceId);
    }

    public Check insert(UUID spaceId, CheckDraft draft) {
        UUID id = UUID.randomUUID();
        var now = clock.instant();
        checks.save(CheckEntity.created(id, spaceId, draft, now));
        return get(spaceId, id);
    }

    public Check update(Check existing, CheckDraft changes, long expectedVersion) {
        var now = clock.instant();
        checks.save(CheckEntity.updated(existing, changes, expectedVersion, now));
        return get(existing.spaceId(), existing.id());
    }

    /** Pauses the supplied entities with one batched write and returns how many really changed. */
    public int pauseAll(List<CheckEntity> targets, PausedReason reason) {
        var now = clock.instant();
        List<CheckEntity> changed = new ArrayList<>(targets.size());
        for (CheckEntity target : targets) {
            boolean unchanged = target.paused() && target.pausedReason() == reason;
            if (!unchanged) {
                changed.add(target.paused(reason, now));
            }
        }
        checks.saveAll(changed);
        return changed.size();
    }

    /** Resumes the supplied entities with one batched write and returns how many really changed. */
    public int resumeAll(List<CheckEntity> targets) {
        var now = clock.instant();
        List<CheckEntity> changed = new ArrayList<>(targets.size());
        for (CheckEntity target : targets) {
            if (target.paused()) {
                changed.add(target.resumed(now));
            }
        }
        checks.saveAll(changed);
        return changed.size();
    }

    /** Deletes the supplied entities by id in one statement; tags and results go through the foreign keys. */
    public int deleteAll(List<CheckEntity> targets) {
        List<UUID> ids = new ArrayList<>(targets.size());
        for (CheckEntity target : targets) {
            ids.add(target.id());
        }
        queries.deleteByIds(ids);
        return ids.size();
    }

    public boolean pause(UUID spaceId, UUID id, PausedReason reason) {
        CheckEntity existing = requiredEntity(spaceId, id);
        boolean unchanged = existing.paused() && existing.pausedReason() == reason;
        if (unchanged) {
            return false;
        }
        var now = clock.instant();
        checks.save(existing.paused(reason, now));
        return true;
    }

    public boolean resume(UUID spaceId, UUID id) {
        CheckEntity existing = requiredEntity(spaceId, id);
        if (!existing.paused()) {
            return false;
        }
        var now = clock.instant();
        checks.save(existing.resumed(now));
        return true;
    }

    public Check delete(UUID spaceId, UUID id) {
        CheckEntity existing = requiredEntity(spaceId, id);
        checks.delete(existing);
        return CheckMapper.toCheck(existing);
    }

    private CheckEntity requiredEntity(UUID spaceId, UUID id) {
        return checks.findByIdAndSpaceId(id, spaceId).orElseThrow(() -> new ApiException(CommonError.NOT_FOUND));
    }
}
