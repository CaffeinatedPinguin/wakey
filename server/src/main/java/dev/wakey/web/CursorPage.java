package dev.wakey.web;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;
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
        String nextCursor) {

    /**
     * Builds a page from up to {@code pageSize + 1} rows; the extra row only signals that another page
     * exists, and the cursor is the sort value of the last row of this page.
     */
    public static <R, T> CursorPage<T> of(
            List<R> rows, int pageSize, Function<R, String> cursorOf, Function<R, T> map) {
        boolean hasMore = rows.size() > pageSize;
        List<R> page = hasMore ? rows.subList(0, pageSize) : rows;
        String nextCursor = null;
        if (hasMore) {
            R last = page.getLast();
            nextCursor = CursorCodec.encode(cursorOf.apply(last));
        }
        List<T> items = page.stream().map(map).toList();
        return new CursorPage<>(items, nextCursor);
    }
}
