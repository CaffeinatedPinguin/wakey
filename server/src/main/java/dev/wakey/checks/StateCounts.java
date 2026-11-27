package dev.wakey.checks;

/** How many checks of a filtered list are in each state, counting a missing result as UNKNOWN. */
public record StateCounts(long up, long degraded, long down, long unknown) {}
