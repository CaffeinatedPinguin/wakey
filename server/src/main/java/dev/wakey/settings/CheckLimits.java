package dev.wakey.settings;

/**
 * The numeric rules shared by a check and its settings. Both the check validator and the settings
 * catalog read them here, so a stored default can never be a value that a check rejects.
 */
public final class CheckLimits {

    public static final int MIN_INTERVAL_SECONDS = 1;

    public static final int MIN_TIMEOUT_SECONDS = 1;

    public static final int MAX_RETRIES = 10;

    private CheckLimits() {}
}
