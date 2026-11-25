package dev.wakey.api.checks;

import dev.wakey.checks.PausedReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** The reason the caller pauses monitoring; the UI sends MANUAL. */
@Schema(description = "The reason to pause monitoring")
public record PauseCheckRequest(
        @Schema(description = "The pause reason", example = "MANUAL") @NotNull
        PausedReason reason) {}
