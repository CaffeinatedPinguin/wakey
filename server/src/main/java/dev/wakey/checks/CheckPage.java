package dev.wakey.checks;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** One page of checks; {@code nextCursor} is null when the page is the last one. */
public record CheckPage(List<Check> items, @Nullable CheckCursor nextCursor) {}
