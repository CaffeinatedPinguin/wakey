package dev.wakey.settings.internal;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.stereotype.Component;

/** The persistence adapter of space settings: Spring Data JDBC rows. */
@Component
@RequiredArgsConstructor
public class SettingStore {

    private final SpaceSettingRepository settings;

    private final JdbcAggregateTemplate aggregates;

    private final Clock clock;

    public Optional<String> findValue(UUID spaceId, String key) {
        SpaceSettingKey id = new SpaceSettingKey(spaceId, key);
        return settings.findById(id).map(SpaceSettingEntity::value);
    }

    public Map<String, String> findAll(UUID spaceId) {
        List<SpaceSettingEntity> rows = settings.findByKeySpaceId(spaceId);
        Map<String, String> values = new LinkedHashMap<>();
        for (SpaceSettingEntity row : rows) {
            SpaceSettingKey id = row.key();
            values.put(id.key(), row.value());
        }
        return values;
    }

    public void save(UUID spaceId, String key, String value) {
        SpaceSettingKey id = new SpaceSettingKey(spaceId, key);
        SpaceSettingEntity entity = new SpaceSettingEntity(id, value, clock.instant());
        if (settings.existsById(id)) {
            aggregates.update(entity);
        } else {
            aggregates.insert(entity);
        }
    }

    public void delete(UUID spaceId, String key) {
        SpaceSettingKey id = new SpaceSettingKey(spaceId, key);
        aggregates.deleteById(id, SpaceSettingEntity.class);
    }
}
