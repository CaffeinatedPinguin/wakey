package dev.wakey.api.checks;

import dev.wakey.checks.StateCounts;
import io.swagger.v3.oas.annotations.media.Schema;

/** How many filtered checks are in each state; a check without results counts as unknown. */
@Schema(description = "How many filtered checks are in each state")
public record StateCountsResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long up,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long degraded,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long down,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long unknown) {

    static StateCountsResponse from(StateCounts counts) {
        return new StateCountsResponse(counts.up(), counts.degraded(), counts.down(), counts.unknown());
    }
}
