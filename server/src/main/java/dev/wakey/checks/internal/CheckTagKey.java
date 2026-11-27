package dev.wakey.checks.internal;

import java.util.UUID;

/** The composite key of a check tag row. */
public record CheckTagKey(UUID checkId, String tag) {}
