package dev.wakey.settings.internal;

import dev.wakey.settings.PlanProperties;
import dev.wakey.settings.SettingError;
import dev.wakey.settings.SpacePlan;
import dev.wakey.web.ApiException;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The resolved plans of this instance: configured plans, or the built-in {@code default} plan with
 * no restrictions beyond the catalog when nothing is configured. A plan defines bounds, never the
 * default value of a setting.
 */
public final class PlanCatalog {

    private static final String BUILT_IN_PLAN = "default";

    private static final PlanProperties UNRESTRICTED = new PlanProperties(true, null, null, null, null);

    private final Map<String, PlanProperties> plans;

    private PlanCatalog(Map<String, PlanProperties> plans) {
        this.plans = plans;
    }

    /** Validates the plans at startup: exactly one is the default when any plan is configured. */
    public static PlanCatalog from(Map<String, PlanProperties> configuredPlans) {
        Map<String, PlanProperties> plans =
                configuredPlans.isEmpty() ? Map.of(BUILT_IN_PLAN, UNRESTRICTED) : Map.copyOf(configuredPlans);
        long defaultPlans =
                plans.values().stream().filter(PlanProperties::defaultPlan).count();
        if (defaultPlans != 1L) {
            throw new IllegalStateException(
                    "Exactly one plan must set wakey.plans.<name>.default=true, but " + defaultPlans + " do");
        }
        return new PlanCatalog(plans);
    }

    /** Resolves a plan by name; an unknown name is a domain error. */
    public PlanProperties plan(String planName) {
        PlanProperties plan = plans.get(planName);
        if (plan == null) {
            throw new ApiException(SettingError.UNKNOWN_PLAN, Map.of("plan", planName));
        }
        return plan;
    }

    /** Checks that a plan exists and returns its name; an unknown name is a domain error. */
    public String requireExisting(String planName) {
        plan(planName);
        return planName;
    }

    /** The name of the configured default plan; a space without an assigned plan resolves to it. */
    public String defaultPlanName() {
        return plans.entrySet().stream()
                .filter(entry -> entry.getValue().defaultPlan())
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow();
    }

    /** Resolves an assigned plan name, falling back to the instance default when none is assigned. */
    public SpacePlan planOf(@Nullable String assignedPlanName) {
        String resolved = nameOrDefault(assignedPlanName);
        return new SpacePlan(resolved, plan(resolved));
    }

    /** Resolves an assigned plan name; a null name means the instance default plan. */
    public String nameOrDefault(@Nullable String planName) {
        return planName == null ? defaultPlanName() : planName;
    }
}
