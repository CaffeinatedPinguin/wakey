package dev.wakey.api.checks;

import dev.wakey.checks.CheckFilter;
import dev.wakey.checks.CheckState;
import dev.wakey.checks.CheckType;
import dev.wakey.checks.PausedReason;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/** The filters shared by the check list and its state counters; a missing field narrows nothing. */
@Schema(description = "The filters shared by the check list and its counters")
public class CheckFilterQuery {

    @Schema(description = "Text matched against the name and the target")
    private @Nullable String q;

    @Schema(description = "The current state, derived from the newest result")
    private @Nullable CheckState state;

    @Schema(description = "The protocol to measure")
    private @Nullable CheckType type;

    @Schema(description = "A tag the check must carry")
    private @Nullable String tag;

    @Schema(description = "The group name")
    private @Nullable String group;

    @Schema(description = "Why monitoring is paused")
    private @Nullable PausedReason pausedReason;

    CheckFilter toFilter() {
        return new CheckFilter(q, state, type, tag, group, pausedReason);
    }

    public @Nullable String getQ() {
        return q;
    }

    public void setQ(@Nullable String q) {
        this.q = q;
    }

    public @Nullable CheckState getState() {
        return state;
    }

    public void setState(@Nullable CheckState state) {
        this.state = state;
    }

    public @Nullable CheckType getType() {
        return type;
    }

    public void setType(@Nullable CheckType type) {
        this.type = type;
    }

    public @Nullable String getTag() {
        return tag;
    }

    public void setTag(@Nullable String tag) {
        this.tag = tag;
    }

    public @Nullable String getGroup() {
        return group;
    }

    public void setGroup(@Nullable String group) {
        this.group = group;
    }

    public @Nullable PausedReason getPausedReason() {
        return pausedReason;
    }

    public void setPausedReason(@Nullable PausedReason pausedReason) {
        this.pausedReason = pausedReason;
    }
}
