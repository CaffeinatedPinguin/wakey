package dev.wakey.checks;

import java.util.UUID;

/** One replacement of an existing check: its id, the new fields, the expected version and the plan bounds. */
public record CheckUpdate(UUID id, CheckDraft changes, long expectedVersion, PlanLimits limits) {}
