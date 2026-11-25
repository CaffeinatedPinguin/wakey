package dev.wakey.api.settings;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** The plan to assign to a space; the API layer checks that the plan exists. */
@Schema(description = "The plan to assign to a space")
public record PutSpacePlanRequest(
        @Schema(description = "A configured plan name", example = "small") @NotBlank
        String plan) {}
