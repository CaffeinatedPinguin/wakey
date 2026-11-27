package dev.wakey.testsupport;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;

/**
 * A clock shared by the integration tests. It follows wall time but never returns the same instant
 * twice, so audit entries keep a stable order, and it can be pinned for a test that needs an exact
 * moment.
 */
public final class TestClock extends Clock {

    private static final long STEP_MILLIS = 1;

    private final AtomicReference<Instant> pinned = new AtomicReference<>();

    private final AtomicReference<Instant> last = new AtomicReference<>(Instant.now());

    /** Pins the clock to the given instant. */
    public void pinAt(Instant instant) {
        pinned.set(instant);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        @Nullable Instant fixed = pinned.get();
        if (fixed != null) {
            return fixed;
        }
        return last.updateAndGet(TestClock::next);
    }

    private static Instant next(Instant previous) {
        Instant candidate = Instant.now().plusMillis(STEP_MILLIS);
        Instant stepped = previous.plusMillis(STEP_MILLIS);
        return candidate.isAfter(stepped) ? candidate : stepped;
    }
}
