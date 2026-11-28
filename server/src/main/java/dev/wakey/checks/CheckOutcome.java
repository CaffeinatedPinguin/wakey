package dev.wakey.checks;

/** The effective state of a check and whether it still needs a configuration review. */
public record CheckOutcome(CheckState state, boolean needsConfigurationReview) {}
