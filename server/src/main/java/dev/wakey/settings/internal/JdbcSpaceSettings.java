package dev.wakey.settings.internal;

import dev.wakey.settings.EffectiveSetting;
import dev.wakey.settings.PlanProperties;
import dev.wakey.settings.SettingCatalog;
import dev.wakey.settings.SettingDefinition;
import dev.wakey.settings.SettingKey;
import dev.wakey.settings.SettingSource;
import dev.wakey.settings.SpaceSettings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

@RequiredArgsConstructor
final class JdbcSpaceSettings implements SpaceSettings {

    private static final RowMapper<StoredSetting> SETTING_MAPPER =
            (rs, unusedRowNum) -> new StoredSetting(rs.getString("key"), rs.getString("value"));

    private static final String SELECT_OVERRIDE_SQL = """
            SELECT value
            FROM space_setting
            WHERE space_id = :spaceId AND key = :key
            """;

    private static final String SELECT_OVERRIDES_SQL = """
            SELECT key, value
            FROM space_setting
            WHERE space_id = :spaceId
            """;

    // The composite primary key makes this the one-statement upsert of a space setting.
    // postgres-only: INSERT ... ON CONFLICT ... DO UPDATE; another database needs its own upsert (for example MERGE).
    private static final String UPSERT_OVERRIDE_SQL = """
            INSERT INTO space_setting (space_id, key, value, updated_at)
            VALUES (:spaceId, :key, :value, CURRENT_TIMESTAMP)
            ON CONFLICT (space_id, key) DO UPDATE
            SET value = EXCLUDED.value, updated_at = CURRENT_TIMESTAMP
            """;

    private static final String DELETE_OVERRIDE_SQL = """
            DELETE FROM space_setting
            WHERE space_id = :spaceId AND key = :key
            """;

    private final JdbcClient jdbc;

    private final PlanCatalog plans;

    @Override
    public EffectiveSetting effective(UUID spaceId, @Nullable String planName, String key) {
        PlanProperties plan = plan(planName);
        SettingDefinition definition = SettingCatalog.require(key);
        String override = findOverride(spaceId, key);
        return resolve(definition, plan, override);
    }

    @Override
    public Map<String, EffectiveSetting> effectiveAll(UUID spaceId, @Nullable String planName) {
        PlanProperties plan = plan(planName);
        Map<String, String> overrides = findOverrides(spaceId);
        Map<String, EffectiveSetting> effective = new LinkedHashMap<>();
        for (SettingDefinition definition : SettingCatalog.all()) {
            SettingKey key = definition.key();
            String override = overrides.get(key.key());
            effective.put(key.key(), resolve(definition, plan, override));
        }
        return effective;
    }

    @Override
    public void set(UUID spaceId, @Nullable String planName, String key, String value) {
        String resolvedPlanName = plans.resolvePlanName(planName);
        PlanProperties plan = plans.plan(resolvedPlanName);
        SettingDefinition definition = SettingCatalog.require(key);
        definition.validate(value);
        plans.checkWithinBound(plan, resolvedPlanName, definition, value);
        jdbc.sql(UPSERT_OVERRIDE_SQL)
                .param("spaceId", spaceId)
                .param("key", key)
                .param("value", value)
                .update();
    }

    @Override
    public void reset(UUID spaceId, String key) {
        SettingCatalog.require(key);
        jdbc.sql(DELETE_OVERRIDE_SQL)
                .param("spaceId", spaceId)
                .param("key", key)
                .update();
    }

    @Override
    public String requireExisting(String planName) {
        return plans.requireExisting(planName);
    }

    @Override
    public String resolvePlanName(@Nullable String planName) {
        return plans.resolvePlanName(planName);
    }

    private PlanProperties plan(@Nullable String planName) {
        return plans.plan(plans.resolvePlanName(planName));
    }

    private EffectiveSetting resolve(SettingDefinition definition, PlanProperties plan, @Nullable String override) {
        EffectiveSetting result;
        if (override != null) {
            result = new EffectiveSetting(override, SettingSource.SPACE);
        } else {
            result = configuredValue(definition, plan);
        }
        return result;
    }

    private EffectiveSetting configuredValue(SettingDefinition definition, PlanProperties plan) {
        String planValue = plans.planValue(definition, plan);
        String instanceValue = plans.instanceValue(definition);
        EffectiveSetting result;
        if (planValue != null) {
            result = new EffectiveSetting(planValue, SettingSource.PLAN);
        } else if (instanceValue != null) {
            result = new EffectiveSetting(instanceValue, SettingSource.INSTANCE);
        } else {
            String defaultValue = definition.defaultValue();
            result = new EffectiveSetting(defaultValue, SettingSource.DEFAULT);
        }
        return result;
    }

    private @Nullable String findOverride(UUID spaceId, String key) {
        return jdbc.sql(SELECT_OVERRIDE_SQL)
                .param("spaceId", spaceId)
                .param("key", key)
                .query(String.class)
                .optional()
                .orElse(null);
    }

    private Map<String, String> findOverrides(UUID spaceId) {
        List<StoredSetting> rows = jdbc.sql(SELECT_OVERRIDES_SQL)
                .param("spaceId", spaceId)
                .query(SETTING_MAPPER)
                .list();
        Map<String, String> overrides = new LinkedHashMap<>();
        for (StoredSetting row : rows) {
            overrides.put(row.key(), row.value());
        }
        return overrides;
    }

    private record StoredSetting(String key, String value) {}
}
