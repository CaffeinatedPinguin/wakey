package dev.wakey.api.settings;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The desired value of one space setting; the settings module owns the format and plan bound. */
@Schema(description = "The desired value of a space setting")
public record SetSpaceSettingRequest(
        @Schema(description = "The desired value", example = "60") @NotBlank @Size(max = 255)
        String value) {}
