package dev.wakey.checks;

import java.util.UUID;

/** The last item already returned, used to resume a list ordered by name and then id. */
public record CheckCursor(String name, UUID id) {}
