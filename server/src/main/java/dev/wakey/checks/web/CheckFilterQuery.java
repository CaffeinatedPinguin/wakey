package dev.wakey.checks.web;

import dev.wakey.checks.CheckFieldLimits;
import dev.wakey.checks.CheckFilter;
import dev.wakey.checks.CheckState;
import dev.wakey.checks.CheckType;
import dev.wakey.checks.PausedReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.annotations.ParameterObject;

/** The filters shared by the check list and its state counters; a missing field narrows nothing. */
@ParameterObject
@Schema(description = "The filters shared by the check list and its counters")
public record CheckFilterQuery(
        @Schema(description = "Text matched against the name and the target")
        @Nullable
        @Size(max = CheckFieldLimits.MAX_NAME_LENGTH)
        String q,

        @Schema(
                description = "The current state, derived from the newest result",
                implementation = CheckState.class,
                enumAsRef = true)
        @Nullable
        CheckState state,

        @Schema(description = "The protocol to measure", implementation = CheckType.class, enumAsRef = true) @Nullable
        CheckType type,

        @Schema(description = "A tag the check must carry") @Nullable @Size(max = CheckFieldLimits.MAX_TAG_LENGTH)
        String tag,

        @Schema(description = "The group name") @Nullable @Size(max = CheckFieldLimits.MAX_GROUP_LENGTH)
        String group,

        @Schema(description = "Why monitoring is paused", implementation = PausedReason.class, enumAsRef = true)
        @Nullable
        PausedReason pausedReason) {

    CheckFilter toFilter() {
        return new CheckFilter(q, state, type, tag, group, pausedReason);
    }
}
