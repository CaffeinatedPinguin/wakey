package dev.wakey.settings.internal;

import java.util.UUID;

/** The composite key of a space setting: {@code (space_id, key)}. */
record SpaceSettingKey(UUID spaceId, String key) {}
