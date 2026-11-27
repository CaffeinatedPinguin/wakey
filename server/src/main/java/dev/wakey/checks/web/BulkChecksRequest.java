package dev.wakey.checks.web;

import dev.wakey.checks.CheckBulkCommand;
import dev.wakey.checks.CheckFilter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** One action applied either to explicit checks or to every check a filter selects. */
@Schema(description = "The bulk action and either the check ids or the filter it applies to")
public record BulkChecksRequest(
        @Schema(
                description = "The action to apply",
                example = "PAUSE",
                implementation = BulkCheckAction.class,
                enumAsRef = true)
        @NotNull
        BulkCheckAction action,

        @Schema(
                description = "The check ids, from " + MIN_IDS + " to " + MAX_IDS
                        + " entries; duplicate ids are applied once",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Nullable
        @Size(min = MIN_IDS, max = MAX_IDS)
        List<@NotNull UUID> ids,

        @Schema(
                description = "The filter that selects every matching check",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Nullable
        @Valid
        CheckFilterQuery filter,

        @Schema(
                description = "The number of checks the caller confirmed in the dialog; required with a filter "
                        + "and must be omitted with ids",
                example = "2",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Nullable
        @Min(0)
        Integer expectedCount) {

    /** The minimum number of explicit ids in one request. */
    public static final int MIN_IDS = 1;

    /** The maximum number of explicit ids in one request. */
    public static final int MAX_IDS = 100;

    CheckBulkCommand toCommand() {
        CheckFilter selected = filter == null ? null : filter.toFilter();
        return new CheckBulkCommand(action.toDomain(), ids, selected, expectedCount);
    }
}
