package dev.wakey.checks;

import java.util.UUID;

/** One replacement of an existing check: its id, the new fields and the expected version. */
public record CheckUpdate(UUID id, CheckDraft changes, long expectedVersion) {}
