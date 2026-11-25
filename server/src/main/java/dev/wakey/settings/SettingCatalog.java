package dev.wakey.settings;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/** The single catalog table; every setting has one definition here instead of its own class. */
@Slf4j
public final class SettingCatalog {

    private static final List<SettingDefinition> DEFINITIONS = List.of(
            new SettingDefinition(SettingKey.TIMEZONE, SettingType.STRING, "UTC", SettingCatalog::isTimeZone),
            new SettingDefinition(SettingKey.LANGUAGE, SettingType.STRING, "pl", SettingCatalog::isLanguage),
            new SettingDefinition(
                    SettingKey.CHECK_INTERVAL, SettingType.INTEGER, "60", SettingCatalog::isNonNegativeInteger),
            new SettingDefinition(
                    SettingKey.CHECK_TIMEOUT, SettingType.INTEGER, "10", SettingCatalog::isNonNegativeInteger),
            new SettingDefinition(
                    SettingKey.CHECK_RETRIES, SettingType.INTEGER, "1", SettingCatalog::isNonNegativeInteger));

    private static final Map<String, SettingDefinition> DEFINITIONS_BY_KEY = indexDefinitions();

    private SettingCatalog() {}

    public static List<SettingDefinition> all() {
        return DEFINITIONS;
    }

    /** Looks up a definition; an unknown key is a domain error, never a silent fallback. */
    public static SettingDefinition require(String key) {
        SettingDefinition definition = DEFINITIONS_BY_KEY.get(key);
        if (definition == null) {
            throw new InvalidSettingException(key, "", "Unknown setting key: " + key);
        }
        return definition;
    }

    private static Map<String, SettingDefinition> indexDefinitions() {
        Map<String, SettingDefinition> definitions = new LinkedHashMap<>();
        for (SettingDefinition definition : DEFINITIONS) {
            SettingKey key = definition.key();
            definitions.put(key.key(), definition);
        }
        return Map.copyOf(definitions);
    }

    private static boolean isTimeZone(String value) {
        try {
            ZoneId.of(value);
            return true;
        } catch (DateTimeException exception) {
            log.debug("{} is not a valid time zone", value, exception);
            return false;
        }
    }

    private static boolean isLanguage(String value) {
        return value.equals("pl") || value.equals("en");
    }

    private static boolean isNonNegativeInteger(String value) {
        try {
            return Long.parseLong(value) >= 0;
        } catch (NumberFormatException exception) {
            log.debug("{} is not a non-negative integer", value, exception);
            return false;
        }
    }
}
