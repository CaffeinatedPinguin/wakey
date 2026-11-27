package dev.wakey.checks;

/** Why monitoring of a check is paused. */
public enum PausedReason {
    MANUAL,
    PLAN_LIMIT,
    ABANDONED,
    UNVERIFIED
}
