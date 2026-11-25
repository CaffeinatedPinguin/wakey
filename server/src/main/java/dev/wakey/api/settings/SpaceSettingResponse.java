package dev.wakey.api.settings;

import dev.wakey.settings.EffectiveSetting;
import dev.wakey.settings.SettingSource;
import io.swagger.v3.oas.annotations.media.Schema;

/** One effective setting of a space, with its source and the plan it was resolved against. */
@Schema(description = "One effective space setting")
public record SpaceSettingResponse(
        @Schema(
                description = "The stable setting key",
                example = "check.interval",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String key,

        @Schema(description = "The effective value", example = "60", requiredMode = Schema.RequiredMode.REQUIRED)
        String value,

        @Schema(description = "Where the effective value comes from", requiredMode = Schema.RequiredMode.REQUIRED)
        SettingSource source,

        @Schema(
                description = "The plan the value was resolved against",
                example = "small",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String planName) {

    static SpaceSettingResponse from(String key, EffectiveSetting setting, String planName) {
        return new SpaceSettingResponse(key, setting.value(), setting.source(), planName);
    }
}
