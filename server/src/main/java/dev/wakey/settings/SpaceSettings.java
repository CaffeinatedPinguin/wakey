package dev.wakey.settings;

import dev.wakey.settings.internal.PlanCatalog;
import dev.wakey.settings.internal.SettingStore;
import dev.wakey.spaces.SpaceService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The settings use cases: the effective value of a space setting inside its plan and the space
 * overrides accepted only within that plan. The service resolves the plan of a space itself, so a
 * caller never passes a plan name.
 */
@Service
@RequiredArgsConstructor
public class SpaceSettings {

    private static final String SET_ACTION = "setting.set";

    private static final String RESET_ACTION = "setting.reset";

    private final SettingStore store;

    private final SpaceService spaceService;

    private final SettingCatalog catalog;

    private final PlanCatalog plans;

    private final SettingAudit audit;

    /** The plan of a space, resolving the instance default when the space has no assigned plan. */
    public SpacePlan planOf(UUID spaceId) {
        @Nullable String assignedPlan = spaceService.planNameOf(spaceId).orElse(null);
        return plans.planOf(assignedPlan);
    }

    /** Reads one effective value: a space override, a plan bound or the instance default. */
    public EffectiveSetting effective(UUID spaceId, String key) {
        SettingDefinition definition = catalog.require(key);
        String override = store.findValue(spaceId, key).orElse(null);
        SpacePlan spacePlan = planOf(spaceId);
        return resolve(definition, spacePlan.properties(), override);
    }

    /** Reads every catalog setting of a space, each with its source. */
    public Map<String, EffectiveSetting> effectiveAll(UUID spaceId) {
        PlanProperties plan = planOf(spaceId).properties();
        Map<String, String> overrides = store.findAll(spaceId);
        Map<String, EffectiveSetting> effective = new LinkedHashMap<>();
        for (SettingDefinition definition : catalog.all()) {
            String key = definition.key().key();
            String override = overrides.get(key);
            EffectiveSetting setting = resolve(definition, plan, override);
            effective.put(key, setting);
        }
        return effective;
    }

    /** Stores a space override; a value outside the plan bound or a bad format is rejected. */
    @Transactional
    public void set(UUID spaceId, SettingValue value, SettingActor actor) {
        SpacePlan spacePlan = planOf(spaceId);
        PlanProperties plan = spacePlan.properties();
        SettingDefinition definition = catalog.require(value.key());
        String canonical = definition.type().parse(value.value());
        definition.validate(canonical);
        definition.requireWithinBound(plan, spacePlan.name(), canonical);
        spaceService.lock(spaceId);
        String oldValue = store.findValue(spaceId, value.key()).orElse(null);
        Map<SettingKey, String> draft = effectiveValues(spaceId, plan, definition.key(), canonical);
        definition.crossCheck(draft);
        store.save(spaceId, value.key(), canonical);
        SettingChange change = new SettingChange(SET_ACTION, spaceId, value.key(), oldValue, canonical, actor);
        audit.settingChanged(change);
    }

    /** Removes a space override, falling back to the plan bound or the instance default. */
    @Transactional
    public void reset(UUID spaceId, String key, SettingActor actor) {
        PlanProperties plan = planOf(spaceId).properties();
        SettingDefinition definition = catalog.require(key);
        spaceService.lock(spaceId);
        String oldValue = store.findValue(spaceId, key).orElse(null);
        store.delete(spaceId, key);
        Map<SettingKey, String> draft = effectiveValues(spaceId, plan, definition.key(), null);
        definition.crossCheck(draft);
        SettingChange change = new SettingChange(RESET_ACTION, spaceId, key, oldValue, null, actor);
        audit.settingChanged(change);
    }

    /**
     * Removes every space override in one transaction, so the space falls back to its plan and the
     * instance defaults. A space without overrides is a no-op.
     */
    @Transactional
    public void resetAll(UUID spaceId, SettingActor actor) {
        spaceService.lock(spaceId);
        Map<String, String> overrides = store.findAll(spaceId);
        for (Map.Entry<String, String> override : overrides.entrySet()) {
            String key = override.getKey();
            store.delete(spaceId, key);
            SettingChange change = new SettingChange(RESET_ACTION, spaceId, key, override.getValue(), null, actor);
            audit.settingChanged(change);
        }
    }

    private EffectiveSetting resolve(SettingDefinition definition, PlanProperties plan, @Nullable String override) {
        if (override != null && definition.withinBound(plan, override)) {
            return new EffectiveSetting(override, SettingSource.SPACE);
        }
        String instanceValue = definition.instanceValue().get();
        return valueOrBound(definition, plan, instanceValue);
    }

    private EffectiveSetting valueOrBound(SettingDefinition definition, PlanProperties plan, String value) {
        Integer bound = definition.bound(plan);
        if (bound != null && !definition.withinBound(plan, value)) {
            return new EffectiveSetting(String.valueOf(bound), SettingSource.PLAN);
        }
        return new EffectiveSetting(value, SettingSource.INSTANCE);
    }

    private Map<SettingKey, String> effectiveValues(
            UUID spaceId, PlanProperties plan, SettingKey changed, @Nullable String draft) {
        Map<String, String> overrides = store.findAll(spaceId);
        Map<SettingKey, String> values = new LinkedHashMap<>();
        for (SettingDefinition definition : catalog.all()) {
            SettingKey key = definition.key();
            String candidate = key.equals(changed) ? draft : overrides.get(key.key());
            EffectiveSetting effective = resolve(definition, plan, candidate);
            values.put(key, effective.value());
        }
        return values;
    }
}
