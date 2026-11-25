package dev.wakey.api.checks;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/** The filters plus the keyset paging of the check list. */
@Schema(description = "The filters plus the keyset paging of the check list")
public final class CheckListQuery extends CheckFilterQuery {

    private static final int DEFAULT_PAGE_SIZE = 20;

    @Schema(description = "The maximum number of items to return", example = "20")
    private @Nullable Integer limit;

    @Schema(description = "The opaque cursor of the next page")
    private @Nullable String cursor;

    int pageSize() {
        return limit == null ? DEFAULT_PAGE_SIZE : limit;
    }

    public @Nullable Integer getLimit() {
        return limit;
    }

    public void setLimit(@Nullable Integer limit) {
        this.limit = limit;
    }

    public @Nullable String getCursor() {
        return cursor;
    }

    public void setCursor(@Nullable String cursor) {
        this.cursor = cursor;
    }
}
