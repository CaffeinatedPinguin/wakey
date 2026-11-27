package dev.wakey.settings;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** One finished setting change: the action and the old and new canonical values. */
record SettingChange(
        String action,
        UUID spaceId,
        String key,
        @Nullable String oldValue,
        @Nullable String newValue,
        SettingActor actor) {}
