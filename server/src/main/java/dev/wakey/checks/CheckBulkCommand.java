package dev.wakey.checks;

import dev.wakey.web.ApiException;
import dev.wakey.web.CommonError;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * One bulk action and its selector: either the explicit ids or a filter that selects every matching
 * check of the space. Exactly one selector is required; a filter also carries the count the caller
 * confirmed.
 */
public record CheckBulkCommand(
        CheckBulkAction action,
        @Nullable List<UUID> ids,
        @Nullable CheckFilter filter,
        @Nullable Integer expectedCount) {

    public CheckBulkCommand {
        if (ids == null && filter == null) {
            throw ApiException.invalidField(CommonError.VALIDATION_FAILED, "/ids");
        }
        if (ids != null && filter != null) {
            throw ApiException.invalidField(CommonError.VALIDATION_FAILED, "/filter");
        }
        if (filter != null && expectedCount == null) {
            throw ApiException.invalidField(CommonError.VALIDATION_FAILED, "/expectedCount");
        }
        if (ids != null && expectedCount != null) {
            throw ApiException.invalidField(CommonError.VALIDATION_FAILED, "/expectedCount");
        }
    }

    /** Refuses the action unless the collected count equals the count the caller confirmed. */
    void requireMatchingCount(int actual) {
        Integer expected = expectedCount;
        if (expected != null && expected != actual) {
            throw new ApiException(CheckError.BULK_COUNT_CHANGED, Map.of("expected", expected, "actual", actual));
        }
    }
}
