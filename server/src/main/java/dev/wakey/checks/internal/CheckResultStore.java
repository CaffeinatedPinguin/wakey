package dev.wakey.checks.internal;

import dev.wakey.checks.CheckResult;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.stereotype.Component;

/** The persistence adapter of the append-only results and the state row they feed. */
@Component
@RequiredArgsConstructor
public class CheckResultStore {

    private final CheckResultRepository results;

    private final CheckRepository checks;

    private final JdbcAggregateOperations aggregates;

    /**
     * Appends one result and copies the state of the newest result (by {@code checked_at} and
     * {@code id}) to the check row. The check row is locked first, so results that arrive from
     * several locations at the same time are written one after another and an older result can
     * never overwrite the state of a newer one.
     */
    public void record(UUID checkId, CheckResult result) {
        var _ = checks.findForUpdateById(checkId).orElseThrow();
        CheckResultEntity entity = CheckResultEntity.from(checkId, result);
        aggregates.insert(entity);
        CheckResultEntity newest = newest(checkId);
        aggregates.update(new CheckStateEntity(checkId, newest.state()));
    }

    public Optional<CheckResult> latest(UUID checkId) {
        Optional<CheckResultEntity> found = results.findFirstByCheckIdOrderByCheckedAtDescIdDesc(checkId);
        return found.map(CheckResultEntity::toResult);
    }

    private CheckResultEntity newest(UUID checkId) {
        return results.findFirstByCheckIdOrderByCheckedAtDescIdDesc(checkId).orElseThrow();
    }
}
