package dev.wakey.settings;

import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The resolved plans of this instance: configured plans, or the built-in {@code default} plan with
 * no restrictions beyond the catalog when nothing is configured. Bounds are also plan values.
 */
public final class PlanCatalog {

    private static final String BUILT_IN_PLAN = "default";

    private static final PlanProperties UNRESTRICTED = new PlanProperties(true, null, null, null, null);

    private final Map<String, PlanProperties> plans;

    private final InstanceDefaultsProperties instanceDefaults;

    private PlanCatalog(Map<String, PlanProperties> plans, InstanceDefaultsProperties instanceDefaults) {
        this.plans = plans;
        this.instanceDefaults = instanceDefaults;
    }

    /** Validates the plans at startup: exactly one is the default when any plan is configured. */
    public static PlanCatalog from(
            Map<String, PlanProperties> configuredPlans, InstanceDefaultsProperties instanceDefaults) {
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
    public PlanProperties plan(String planName) {
        PlanProperties plan = plans.get(planName);
        if (plan == null) {
            throw new UnknownPlanException(planName);
        }
        return plan;
    }

    /** The plan value of a setting: the bound associated with its key, when the plan defines it. */
    public @Nullable String planValue(PlanProperties plan, SettingKey key) {
        return number(bound(plan, key));
    }

    /** The instance default of a setting from {@code wakey.defaults.*}. */
    public @Nullable String instanceValue(SettingKey key) {
        InstanceDefaultsProperties.CheckDefaults check = instanceDefaults.check();
        return switch (key) {
            case TIMEZONE -> instanceDefaults.timezone();
            case LANGUAGE -> instanceDefaults.language();
            case CHECK_INTERVAL -> number(check == null ? null : check.interval());
            case CHECK_TIMEOUT -> number(check == null ? null : check.timeout());
            case CHECK_RETRIES -> number(check == null ? null : check.retries());
            default -> null;
        };
    }

    /** Rejects a numeric value outside the plan bound with the bound in the exception. */
    public void checkWithinBound(PlanProperties plan, String planName, SettingKey key, String value) {
        Integer bound = bound(plan, key);
        if (bound == null) {
            return;
        }
        long parsed = Long.parseLong(value);
        boolean outside = key == SettingKey.CHECK_INTERVAL ? parsed < bound : parsed > bound;
        if (outside) {
            throw new SettingOutOfPlanException(key.key(), parsed, bound, planName);
        }
    }

    private static @Nullable Integer bound(PlanProperties plan, SettingKey key) {
        return switch (key) {
            case CHECK_INTERVAL -> plan.minInterval();
            case CHECK_TIMEOUT -> plan.maxTimeout();
            case CHECK_RETRIES -> plan.maxRetries();
            default -> null;
        };
    }

    private static @Nullable String number(@Nullable Integer value) {
        return value == null ? null : String.valueOf(value);
    }
}
