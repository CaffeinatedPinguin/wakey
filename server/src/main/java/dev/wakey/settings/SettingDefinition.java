package dev.wakey.settings;

import dev.wakey.web.ApiException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

/**
 * One catalog entry: its key, the descriptor fields a frontend renders, the catalog bounds, its
 * validation rule, the side of its plan bound, the instance default and an optional cross-check
 * over the effective values of a space.
 */
@Slf4j
public record SettingDefinition(
        SettingKey key,
        SettingGroup group,
        SettingType type,
        @Nullable String unit,
        @Nullable List<Language> options,
        @Nullable Long minimum,
        @Nullable Long maximum,
        Predicate<String> validator,
        BoundDirection direction,
        Function<PlanProperties, @Nullable Integer> planBound,
        Supplier<String> instanceValue,
        @Nullable Consumer<Map<SettingKey, String>> crossCheck) {

    /** Rejects a value that does not follow the catalog rule for this key. */
    public void validate(String value) {
        if (!validator.test(value)) {
            throw ApiException.invalidField(SettingError.INVALID_VALUE, "/value");
        }
    }

    /** The plan bound of this key, when the plan defines one. */
    public @Nullable Integer bound(PlanProperties plan) {
        return planBound.apply(plan);
    }

    /** Whether a value respects the plan bound of this key; an absent bound accepts everything. */
    public boolean withinBound(PlanProperties plan, String value) {
        Integer bound = bound(plan);
        if (bound == null) {
            return true;
        }
        long parsed = type.number(value);
        return direction == BoundDirection.MIN ? parsed >= bound : parsed <= bound;
    }

    /** Rejects a value outside the plan bound, carrying the bound and the plan in the error params. */
    public void requireWithinBound(PlanProperties plan, String planName, String value) {
        Integer bound = bound(plan);
        if (bound != null && !withinBound(plan, value)) {
            log.debug("Setting {} value {} is outside the {} bound of plan {}", key.key(), value, bound, planName);
            Map<String, Object> params = Map.of("bound", bound, "planName", planName, "key", key.key());
            throw new ApiException(SettingError.OUT_OF_PLAN, params);
        }
    }

    /** The catalog minimum combined with a minimum plan bound; null when neither is set. */
    public @Nullable Long effectiveMin(PlanProperties plan) {
        Integer planBoundValue = bound(plan);
        if (planBoundValue == null || direction != BoundDirection.MIN) {
            return minimum;
        }
        long boundValue = planBoundValue;
        return minimum == null ? boundValue : Math.max(minimum, boundValue);
    }

    /** The catalog maximum combined with a maximum plan bound; null when neither is set. */
    public @Nullable Long effectiveMax(PlanProperties plan) {
        Integer planBoundValue = bound(plan);
        if (planBoundValue == null || direction != BoundDirection.MAX) {
            return maximum;
        }
        long boundValue = planBoundValue;
        return maximum == null ? boundValue : Math.min(maximum, boundValue);
    }

    /** Runs the cross-check of this key on the effective values of the space, when it has one. */
    public void crossCheck(Map<SettingKey, String> effectiveValues) {
        if (crossCheck != null) {
            crossCheck.accept(effectiveValues);
        }
    }
}
