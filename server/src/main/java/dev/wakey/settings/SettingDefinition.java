package dev.wakey.settings;

import java.util.function.Predicate;

/** One catalog entry: its stable key, value shape, default and validation rule. */
public record SettingDefinition(SettingKey key, SettingType type, String defaultValue, Predicate<String> validator) {

    /** Rejects a value that does not follow the catalog rule for this key. */
    public void validate(String value) {
        if (!validator.test(value)) {
            String message = "Invalid value for setting " + key.key();
            throw new InvalidSettingException(key.key(), value, message);
        }
    }
}
