package dev.wakey.checks.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs the checks of one scheduler cycle on virtual threads. The semaphore bounds how many
 * measurements run at the same time; the cycle waits for all of them so no check of the same
 * selection starts twice.
 */
@Slf4j
final class CheckSchedulerTasks implements AutoCloseable {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private final Semaphore permits;

    CheckSchedulerTasks(int parallelism) {
        this.permits = new Semaphore(parallelism);
    }

    void runAll(List<Runnable> work) {
        List<Future<?>> futures = new ArrayList<>(work.size());
        for (Runnable task : work) {
            futures.add(executor.submit(() -> runOne(task)));
        }
        await(futures);
    }

    private void runOne(Runnable task) {
        boolean acquired = acquire();
        if (!acquired) {
            return;
        }
        try {
            task.run();
        } finally {
            permits.release();
        }
    }

    private boolean acquire() {
        try {
            permits.acquire();
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.debug("Check scheduler task was interrupted before it started", exception);
            return false;
        }
    }

    private static void await(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                log.debug("Check scheduler cycle was interrupted", exception);
                return;
            } catch (ExecutionException exception) {
                log.warn("Check scheduler task failed", exception.getCause());
            }
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
