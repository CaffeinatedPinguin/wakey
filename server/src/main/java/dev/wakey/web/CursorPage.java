package dev.wakey.web;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** A keyset page: the items plus the cursor of the next page, or {@code null} on the last page. */
public record CursorPage<T>(List<T> items, @Nullable String nextCursor) {}
