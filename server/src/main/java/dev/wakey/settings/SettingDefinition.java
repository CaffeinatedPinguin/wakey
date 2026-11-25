package dev.wakey.settings;

import java.util.function.Function;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/**
 * One catalog entry: its key, value shape, default, validation rule, the side of its plan bound and
 * the instance default.
 */
public record SettingDefinition(
        SettingKey key,
        SettingType type,
        String defaultValue,
        Predicate<String> validator,
        BoundDirection direction,
        Function<PlanProperties, @Nullable Integer> planBound,
        Function<InstanceDefaultsProperties, @Nullable String> instanceValue) {

    /** Rejects a value that does not follow the catalog rule for this key. */
    public void validate(String value) {
        if (!validator.test(value)) {
            String message = "Invalid value for setting " + key.key();
            throw new InvalidSettingException(key.key(), value, message);
        }
    }

    /** The plan bound of this key, when the plan defines one. */
    public @Nullable Integer bound(PlanProperties plan) {
        return planBound.apply(plan);
    }
}
