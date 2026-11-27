package dev.wakey.checks;

/**
 * The numeric rules shared by a check and its settings definitions. Both the check validator and
 * the check settings read them here, so a stored default can never be a value that a check rejects.
 */
public final class CheckLimits {

    public static final int MIN_INTERVAL_SECONDS = 1;

    public static final int MIN_TIMEOUT_SECONDS = 1;

    public static final int MIN_RETRIES = 0;

    public static final int MAX_RETRIES = 10;

    private CheckLimits() {}

    /** The one rule for a timeout: it may never exceed the interval it belongs to. */
    public static boolean timeoutWithinInterval(long timeoutSeconds, long intervalSeconds) {
        return timeoutSeconds <= intervalSeconds;
    }
}
