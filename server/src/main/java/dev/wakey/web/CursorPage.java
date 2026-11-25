package dev.wakey.web;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** A keyset page: the items plus the cursor of the next page, or {@code null} on the last page. */
public record CursorPage<T>(
        @Schema(description = "The items on this page", requiredMode = Schema.RequiredMode.REQUIRED)
        List<T> items,

        @Schema(
                description = "The cursor of the next page, or null on the last page",
                requiredMode = Schema.RequiredMode.REQUIRED,
                nullable = true)
        @Nullable
        String nextCursor) {}
