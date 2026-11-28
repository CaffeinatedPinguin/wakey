package dev.wakey.checks.internal;

import dev.wakey.checks.CheckResult;
import dev.wakey.checks.CheckState;
import dev.wakey.checks.ErrorCategory;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.stereotype.Component;

/** The persistence adapter of the append-only results, the state row and the state intervals. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CheckResultStore {

    private final CheckResultRepository results;

    private final CheckRepository checks;

    private final CheckStateIntervalRepository intervals;

    private final JdbcAggregateOperations aggregates;

    /**
     * Appends one result and copies the state of the newest result (by {@code checked_at} and
     * {@code id}) to the check row. The check row is locked first, so results that arrive from
     * several locations at the same time are written one after another and an older result can
     * never overwrite the state of a newer one.
     */
    public void record(UUID checkId, CheckResult result) {
        CheckEntity check = checks.findForUpdateById(checkId).orElseThrow();
        CheckResultEntity entity = CheckResultEntity.from(checkId, result);
        aggregates.insert(entity);
        CheckResultEntity newest = newest(checkId);
        updateState(check, newest);
    }

    public Optional<CheckResult> latest(UUID checkId) {
        Optional<CheckResultEntity> found = results.findFirstByCheckIdOrderByCheckedAtDescIdDesc(checkId);
        return found.map(CheckResultEntity::toResult);
    }

    public List<CheckResult> recent(UUID checkId, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<CheckResultEntity> rows = results.findByCheckIdOrderByCheckedAtDescIdDesc(checkId, Limit.of(limit));
        return rows.stream().map(CheckResultEntity::toResult).toList();
    }

    public boolean hasResult(UUID checkId, Instant checkedAt) {
        return results.existsByCheckIdAndCheckedAt(checkId, checkedAt);
    }

    /**
     * A result is successful when the measurement itself worked; a location or authentication
     * failure does not prove that the target answered.
     */
    public boolean hasSuccessfulResult(UUID checkId) {
        return results.existsByCheckIdAndErrorCategoryIsNull(checkId)
                || results.existsByCheckIdAndErrorCategory(checkId, ErrorCategory.TARGET_DOWN);
    }

    private CheckResultEntity newest(UUID checkId) {
        return results.findFirstByCheckIdOrderByCheckedAtDescIdDesc(checkId).orElseThrow();
    }

    private void updateState(CheckEntity check, CheckResultEntity newest) {
        CheckState next = newest.state();
        Instant at = newest.checkedAt();
        Optional<CheckStateIntervalEntity> open =
                intervals.findFirstByCheckIdAndEndedAtIsNullOrderByStartedAtDesc(check.id());
        if (open.isEmpty() || open.get().state() != next) {
            open.ifPresent(interval -> aggregates.update(interval.endedAt(at)));
            CheckStateIntervalEntity interval = CheckStateIntervalEntity.open(check.id(), check.spaceId(), next, at);
            aggregates.insert(interval);
        }
        aggregates.update(new CheckStateEntity(check.id(), next));
        if (check.lastState() != next) {
            log.info(
                    "Check {} in space {} changed state {} -> {}",
                    check.id(),
                    check.spaceId(),
                    check.lastState(),
                    next);
        }
    }
}
