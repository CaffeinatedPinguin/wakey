package dev.wakey.checks.web;

import dev.wakey.checks.CheckFieldLimits;
import dev.wakey.checks.CheckFilter;
import dev.wakey.checks.CheckPosition;
import dev.wakey.checks.CheckState;
import dev.wakey.checks.CheckType;
import dev.wakey.checks.PausedReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.annotations.ParameterObject;

/** The filters plus the keyset paging of the check list. */
@ParameterObject
@Schema(description = "The filters plus the keyset paging of the check list")
public record CheckListQuery(
        @Schema(description = "The opaque cursor of the next page") @Nullable
        String cursor,

        @Schema(description = "Text matched against the name and the target")
        @Nullable
        @Size(max = CheckFieldLimits.MAX_NAME_LENGTH)
        String q,

        @Schema(description = "The current state, derived from the newest result") @Nullable
        CheckState state,

        @Schema(description = "The protocol to measure") @Nullable
        CheckType type,

        @Schema(description = "A tag the check must carry") @Nullable @Size(max = CheckFieldLimits.MAX_TAG_LENGTH)
        String tag,

        @Schema(description = "The group name") @Nullable @Size(max = CheckFieldLimits.MAX_GROUP_LENGTH)
        String group,

        @Schema(description = "Why monitoring is paused") @Nullable
        PausedReason pausedReason) {

    CheckFilter toFilter() {
        CheckFilterQuery filters = new CheckFilterQuery(q, state, type, tag, group, pausedReason);
        return filters.toFilter();
    }

    @Nullable
    CheckPosition after() {
        return CheckListCodec.decode(cursor);
    }
}
