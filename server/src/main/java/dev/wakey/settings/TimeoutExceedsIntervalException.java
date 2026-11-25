package dev.wakey.settings;

/** Rejects a timeout longer than the interval or an interval shorter than the timeout. */
public final class TimeoutExceedsIntervalException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TimeoutExceedsIntervalException() {
        super("A check timeout must not exceed the interval");
    }
}
