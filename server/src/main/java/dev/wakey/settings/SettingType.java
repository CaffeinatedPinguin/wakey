package dev.wakey.settings;

/** The value shape of a catalog entry; it owns parsing and canonicalisation. */
public enum SettingType {
    STRING,
    INTEGER,
    ENUM;

    /** Parses a raw value and returns its canonical form; a bad integer is a validation error. */
    public String parse(String value) {
        if (this == INTEGER) {
            return String.valueOf(number(value));
        }
        return value;
    }

    /** Reads a canonical integer value; this is the only place a stored number is parsed. */
    public long number(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw SettingErrors.invalidValue(exception);
        }
    }
}
