package dev.wakey.settings.internal;

import dev.wakey.settings.InstanceDefaultsProperties;
import dev.wakey.settings.PlanProperties;
import dev.wakey.settings.SettingDefinition;
import dev.wakey.settings.SettingKey;
import dev.wakey.settings.SettingOutOfPlanException;
import dev.wakey.settings.UnknownPlanException;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The resolved plans of this instance: configured plans, or the built-in {@code default} plan with
 * no restrictions beyond the catalog when nothing is configured. Bounds are also plan values.
 */
final class PlanCatalog {

    private static final String BUILT_IN_PLAN = "default";

    private static final PlanProperties UNRESTRICTED = new PlanProperties(true, null, null, null, null);

    private final Map<String, PlanProperties> plans;

    private final InstanceDefaultsProperties instanceDefaults;

    private PlanCatalog(Map<String, PlanProperties> plans, InstanceDefaultsProperties instanceDefaults) {
        this.plans = plans;
        this.instanceDefaults = instanceDefaults;
    }

    /** Validates the plans at startup: exactly one is the default when any plan is configured. */
    static PlanCatalog from(Map<String, PlanProperties> configuredPlans, InstanceDefaultsProperties instanceDefaults) {
        Map<String, PlanProperties> plans =
                configuredPlans.isEmpty() ? Map.of(BUILT_IN_PLAN, UNRESTRICTED) : Map.copyOf(configuredPlans);
        long defaultPlans =
                plans.values().stream().filter(PlanProperties::defaultPlan).count();
        if (defaultPlans != 1L) {
            throw new IllegalStateException(
                    "Exactly one plan must set wakey.plans.<name>.default=true, but " + defaultPlans + " do");
        }
        return new PlanCatalog(plans, instanceDefaults);
    }

    /** Resolves a plan by name; an unknown name is a domain error. */
    PlanProperties plan(String planName) {
        PlanProperties plan = plans.get(planName);
        if (plan == null) {
            throw new UnknownPlanException(planName);
        }
        return plan;
    }

    /** Checks that a plan exists and returns its name; an unknown name is a domain error. */
    String requireExisting(String planName) {
        plan(planName);
        return planName;
    }

    /** The name of the configured default plan; a space without an assigned plan resolves to it. */
    String defaultPlanName() {
        return plans.entrySet().stream()
                .filter(entry -> entry.getValue().defaultPlan())
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow();
    }

    /** Resolves an assigned plan name; a null name means the instance default plan. */
    String resolvePlanName(@Nullable String planName) {
        return planName == null ? defaultPlanName() : planName;
    }

    /** The plan value of a setting: the bound associated with its key, when the plan defines it. */
    @Nullable
    String planValue(SettingDefinition definition, PlanProperties plan) {
        return number(definition.bound(plan));
    }

    /** The instance default of a setting from {@code wakey.defaults.*}. */
    @Nullable
    String instanceValue(SettingDefinition definition) {
        return definition.instanceValue().apply(instanceDefaults);
    }

    /** Rejects a numeric value outside the plan bound with the bound in the exception. */
    void checkWithinBound(PlanProperties plan, String planName, SettingDefinition definition, String value) {
        Integer bound = definition.bound(plan);
        if (bound == null) {
            return;
        }
        long parsed = Long.parseLong(value);
        boolean outside = definition.key() == SettingKey.CHECK_INTERVAL ? parsed < bound : parsed > bound;
        if (outside) {
            String key = definition.key().key();
            throw new SettingOutOfPlanException(key, parsed, bound, planName);
        }
    }

    private static @Nullable String number(@Nullable Integer value) {
        return value == null ? null : String.valueOf(value);
    }
}
