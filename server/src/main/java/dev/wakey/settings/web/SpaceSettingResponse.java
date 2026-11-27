package dev.wakey.settings.web;

import dev.wakey.settings.EffectiveSetting;
import dev.wakey.settings.Language;
import dev.wakey.settings.SettingDefinition;
import dev.wakey.settings.SettingGroup;
import dev.wakey.settings.SettingSource;
import dev.wakey.settings.SettingType;
import dev.wakey.settings.SpacePlan;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** One effective setting of a space with the descriptor a frontend renders it from. */
@Schema(description = "One effective space setting")
public record SpaceSettingResponse(
        @Schema(
                description = "The stable setting key",
                example = "check.interval",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String key,

        @Schema(description = "The effective value", example = "60", requiredMode = Schema.RequiredMode.REQUIRED)
        String value,

        @Schema(
                description = "Where the effective value comes from",
                requiredMode = Schema.RequiredMode.REQUIRED,
                implementation = SettingSource.class,
                enumAsRef = true)
        SettingSource source,

        @Schema(
                description = "The plan the value was resolved against",
                example = "small",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String planName,

        @Schema(
                description = "The value shape",
                requiredMode = Schema.RequiredMode.REQUIRED,
                implementation = SettingType.class,
                enumAsRef = true)
        SettingType type,

        @Schema(
                description = "The unit of the value, or null",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                nullable = true)
        @Nullable
        String unit,

        @Schema(
                description = "The allowed values of an enum setting, or null",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                nullable = true)
        @Nullable
        List<Language> options,

        @Schema(
                description = "The effective lower bound, or null",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                nullable = true)
        @Nullable
        Long min,

        @Schema(
                description = "The effective upper bound, or null",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                nullable = true)
        @Nullable
        Long max,

        @Schema(
                description = "The group the setting is rendered in",
                requiredMode = Schema.RequiredMode.REQUIRED,
                implementation = SettingGroup.class,
                enumAsRef = true)
        SettingGroup group) {

    static SpaceSettingResponse from(SettingDefinition definition, EffectiveSetting setting, SpacePlan plan) {
        String key = definition.key().key();
        var planProperties = plan.properties();
        return new SpaceSettingResponse(
                key,
                setting.value(),
                setting.source(),
                plan.name(),
                definition.type(),
                definition.unit(),
                definition.options(),
                definition.effectiveMin(planProperties),
                definition.effectiveMax(planProperties),
                definition.group());
    }
}
