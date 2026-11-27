package dev.wakey.checks;

/** Why a measurement failed; a location network error never makes the target down. */
public enum ErrorCategory {
    TARGET_DOWN,
    LOCATION_NETWORK,
    AUTH
}
