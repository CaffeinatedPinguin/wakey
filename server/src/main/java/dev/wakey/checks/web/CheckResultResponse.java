package dev.wakey.checks.web;

import dev.wakey.checks.CheckResult;
import dev.wakey.checks.CheckState;
import dev.wakey.checks.ErrorCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/** The newest measurement of a check, or the absence of any measurement. */
@Schema(description = "The newest measurement of a check")
public record CheckResultResponse(
        @Schema(description = "The location that measured the check", example = "server") @Nullable
        String locationId,

        @Schema(description = "When the measurement happened", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant checkedAt,

        @Schema(
                description = "The state of the measurement",
                requiredMode = Schema.RequiredMode.REQUIRED,
                implementation = CheckState.class,
                enumAsRef = true)
        CheckState state,

        @Schema(description = "Why a failed measurement failed", implementation = ErrorCategory.class, enumAsRef = true)
        @Nullable
        ErrorCategory errorCategory,

        @Schema(description = "The HTTP status code of an HTTP check") @Nullable
        Integer httpStatus,

        @Schema(description = "How long the measurement took in milliseconds") @Nullable
        Integer durationMs,

        @Schema(description = "The negotiated protocol version") @Nullable
        String protocolVersion,

        @Schema(description = "The measured phases as a JSON document") @Nullable
        String phases) {

    static CheckResultResponse from(CheckResult result) {
        return new CheckResultResponse(
                result.locationId(),
                result.checkedAt(),
                result.state(),
                result.errorCategory(),
                result.httpStatus(),
                result.durationMs(),
                result.protocolVersion(),
                result.phases());
    }
}
