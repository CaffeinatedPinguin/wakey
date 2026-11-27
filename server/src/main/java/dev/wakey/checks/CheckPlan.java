package dev.wakey.checks;

import dev.wakey.checks.internal.CheckCapacity;
import dev.wakey.settings.PlanProperties;
import dev.wakey.settings.SettingCatalog;
import dev.wakey.settings.SettingDefinition;
import dev.wakey.settings.SettingKey;
import dev.wakey.settings.SpacePlan;
import dev.wakey.settings.SpaceSettings;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Applies the space plan to a check: the setting bounds and the check capacity. */
@Component
@RequiredArgsConstructor
public class CheckPlan {

    private final SpaceSettings settings;

    private final SettingCatalog catalog;

    private final CheckCapacity capacity;

    /** Resolves the plan of the space, validates the bounds and enforces the check capacity. */
    public void requireForNew(UUID spaceId, CheckDraft draft) {
        SpacePlan plan = settings.planOf(spaceId);
        requireWithinPlan(plan, draft);
        PlanProperties properties = plan.properties();
        String planName = plan.name();
        capacity.require(spaceId, properties, planName);
    }

    /** Resolves the plan of the space and validates the bounds of an updated check. */
    public void requireForUpdate(UUID spaceId, CheckDraft draft) {
        requireWithinPlan(settings.planOf(spaceId), draft);
    }

    private void requireWithinPlan(SpacePlan plan, CheckDraft draft) {
        requireWithinBound(plan, CheckSettingKeys.INTERVAL, draft.intervalSeconds());
        requireWithinBound(plan, CheckSettingKeys.TIMEOUT, draft.timeoutSeconds());
        requireWithinBound(plan, CheckSettingKeys.RETRIES, draft.retries());
    }

    private void requireWithinBound(SpacePlan plan, SettingKey key, int value) {
        String text = String.valueOf(value);
        SettingDefinition definition = catalog.require(key.key());
        PlanProperties properties = plan.properties();
        String planName = plan.name();
        definition.requireWithinBound(properties, planName, text);
    }
}
