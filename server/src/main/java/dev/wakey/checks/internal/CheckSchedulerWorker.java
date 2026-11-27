package dev.wakey.checks.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/** Runs the scheduler on a virtual thread, only as long as this instance is the leader. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "wakey.checks.scheduler.enabled", havingValue = "true", matchIfMissing = true)
class CheckSchedulerWorker implements SmartLifecycle {

    private static final long POLL_INTERVAL_MILLIS = 500L;

    private final CheckSchedulerLoop loop;

    private volatile boolean running;

    @Nullable
    private Thread thread;

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        thread = Thread.ofVirtual().name("check-scheduler").start(this::poll);
    }

    @Override
    public synchronized void stop() {
        running = false;
        @Nullable Thread current = thread;
        if (current != null) {
            current.interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void poll() {
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    loop.runOnce();
                } catch (DataAccessException | IllegalStateException exception) {
                    log.warn("Check scheduler cycle failed", exception);
                }
                sleep();
            }
        } finally {
            running = false;
        }
    }

    private void sleep() {
        try {
            Thread.sleep(POLL_INTERVAL_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.debug("Check scheduler worker was interrupted", exception);
        }
    }
}
