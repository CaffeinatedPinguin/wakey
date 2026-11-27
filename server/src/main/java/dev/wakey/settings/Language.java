package dev.wakey.settings;

import com.fasterxml.jackson.annotation.JsonValue;

/** The supported languages; the code is the stored and reported setting value. */
public enum Language {
    PL("pl"),
    EN("en");

    private final String code;

    Language(String code) {
        this.code = code;
    }

    /** The stored code; it is also the JSON form of the enum, so the contract lists {@code pl}/{@code en}. */
    @JsonValue
    public String code() {
        return code;
    }

    /** Whether the given stored value names a supported language. */
    public static boolean isSupported(String value) {
        for (Language language : values()) {
            if (language.code.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
