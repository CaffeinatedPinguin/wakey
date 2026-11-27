package dev.wakey.checks.internal;

import dev.wakey.checks.Check;
import dev.wakey.checks.CheckExecutors;
import dev.wakey.checks.CheckResult;
import dev.wakey.checks.CheckResults;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs the checks that are due at the cycle moment through the executor of their type on bounded
 * virtual threads and publishes every produced result in its own short transaction. Results of
 * checks deleted or paused after selection are dropped instead of published.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class CheckScheduler {

    private final CheckStore store;

    private final CheckExecutors executors;

    private final CheckResults results;

    private final PlatformTransactionManager transactions;

    private final CheckSchedulerTasks tasks;

    private final Clock clock;

    void runCycle() {
        Instant now = clock.instant();
        List<CheckEntity> due = store.due(now);
        log.debug("Scheduler cycle selected {} due checks", due.size());
        List<Runnable> work = new ArrayList<>(due.size());
        for (CheckEntity entity : due) {
            Check check = CheckMapper.toCheck(entity);
            work.add(() -> execute(check));
        }
        tasks.runAll(work);
    }

    private void execute(Check check) {
        log.debug("Starting check {}", check.id());
        long startedNanos = System.nanoTime();
        try {
            CheckResult result = executors.executorFor(check.type()).execute(check);
            long durationMillis = (System.nanoTime() - startedNanos) / 1_000_000L;
            log.debug("Finished check {} with state {} in {} ms", check.id(), result.state(), durationMillis);
            publish(check, result);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            log.warn("Check {} failed before it produced a result", check.id(), exception);
        }
    }

    private void publish(Check check, CheckResult result) {
        TransactionTemplate template = new TransactionTemplate(transactions);
        template.executeWithoutResult(status -> {
            boolean active = store.active(check.id()).isPresent();
            if (active) {
                results.publish(check.id(), check.spaceId(), result);
            } else {
                log.debug("Skipping the result of check {}: it was deleted or paused", check.id());
            }
        });
    }
}
