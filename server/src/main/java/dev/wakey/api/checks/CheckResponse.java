package dev.wakey.api.checks;

import dev.wakey.checks.Check;
import dev.wakey.checks.CheckResult;
import dev.wakey.checks.CheckState;
import dev.wakey.checks.CheckType;
import dev.wakey.checks.PausedReason;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** A check as seen by the caller, optionally with its newest result. */
@Schema(description = "A check as seen by the caller")
public record CheckResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CheckType type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String target,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int intervalSeconds,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int timeoutSeconds,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int retries,

        @Schema(description = "The optional group name") @Nullable
        String groupName,

        @Schema(description = "The optional description") @Nullable
        String description,

        @Schema(description = "The optional runbook address") @Nullable
        String runbookUrl,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean paused,

        @Schema(description = "Why monitoring is paused, if it is") @Nullable
        PausedReason pausedReason,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> tags,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CheckState state,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long version,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant updatedAt,

        @Schema(description = "The newest result, present only when the caller reads one check") @Nullable
        CheckResultResponse lastResult) {

    static CheckResponse from(Check check) {
        return from(check, null);
    }

    static CheckResponse from(Check check, @Nullable CheckResult lastResult) {
        @Nullable CheckResultResponse result = lastResult == null ? null : CheckResultResponse.from(lastResult);
        return new CheckResponse(
                check.id(),
                check.name(),
                check.type(),
                check.target(),
                check.intervalSeconds(),
                check.timeoutSeconds(),
                check.retries(),
                check.groupName(),
                check.description(),
                check.runbookUrl(),
                check.paused(),
                check.pausedReason(),
                check.tags(),
                check.state(),
                check.version(),
                check.createdAt(),
                check.updatedAt(),
                result);
    }
}
