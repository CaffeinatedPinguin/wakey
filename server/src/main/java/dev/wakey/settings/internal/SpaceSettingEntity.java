package dev.wakey.settings.internal;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

/** One space setting row, keyed by the space and the setting key. */
@Table("space_setting")
record SpaceSettingEntity(
        @Id @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY) SpaceSettingKey key, String value, Instant updatedAt) {}
