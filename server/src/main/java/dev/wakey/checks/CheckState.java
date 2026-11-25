package dev.wakey.checks;

/** The result of one measurement; a check without any result is {@link #UNKNOWN}. */
public enum CheckState {
    UP,
    DEGRADED,
    DOWN,
    UNKNOWN
}
