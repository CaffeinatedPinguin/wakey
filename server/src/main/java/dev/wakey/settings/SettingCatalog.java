package dev.wakey.settings;

import dev.wakey.checks.CheckLimits;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

/** The single catalog table; every setting has one definition here instead of its own class. */
@Slf4j
public final class SettingCatalog {

    private static final List<SettingDefinition> DEFINITIONS = List.of(
            new SettingDefinition(
                    SettingKey.TIMEZONE,
                    SettingType.STRING,
                    "UTC",
                    SettingCatalog::isTimeZone,
                    BoundDirection.MAX,
                    plan -> null,
                    InstanceDefaultsProperties::timezone),
            new SettingDefinition(
                    SettingKey.LANGUAGE,
                    SettingType.STRING,
                    "pl",
                    SettingCatalog::isLanguage,
                    BoundDirection.MAX,
                    plan -> null,
                    InstanceDefaultsProperties::language),
            new SettingDefinition(
                    SettingKey.CHECK_INTERVAL,
                    SettingType.INTEGER,
                    "60",
                    value -> isIntegerAtLeast(value, CheckLimits.MIN_INTERVAL_SECONDS),
                    BoundDirection.MIN,
                    PlanProperties::minInterval,
                    SettingCatalog::intervalDefault),
            new SettingDefinition(
                    SettingKey.CHECK_TIMEOUT,
                    SettingType.INTEGER,
                    "10",
                    value -> isIntegerAtLeast(value, CheckLimits.MIN_TIMEOUT_SECONDS),
                    BoundDirection.MAX,
                    PlanProperties::maxTimeout,
                    SettingCatalog::timeoutDefault),
            new SettingDefinition(
                    SettingKey.CHECK_RETRIES,
                    SettingType.INTEGER,
                    "1",
                    value -> isIntegerBetween(value, 0, CheckLimits.MAX_RETRIES),
                    BoundDirection.MAX,
                    PlanProperties::maxRetries,
                    SettingCatalog::retriesDefault));

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

    private static boolean isIntegerAtLeast(String value, int minimum) {
        try {
            return Long.parseLong(value) >= minimum;
        } catch (NumberFormatException exception) {
            log.debug("{} is not an integer of at least {}", value, minimum, exception);
            return false;
        }
    }

    private static boolean isIntegerBetween(String value, int minimum, int maximum) {
        try {
            long parsed = Long.parseLong(value);
            return parsed >= minimum && parsed <= maximum;
        } catch (NumberFormatException exception) {
            log.debug("{} is not an integer between {} and {}", value, minimum, maximum, exception);
            return false;
        }
    }

    private static @Nullable String intervalDefault(InstanceDefaultsProperties defaults) {
        InstanceDefaultsProperties.CheckDefaults check = defaults.check();
        return number(check == null ? null : check.interval());
    }

    private static @Nullable String timeoutDefault(InstanceDefaultsProperties defaults) {
        InstanceDefaultsProperties.CheckDefaults check = defaults.check();
        return number(check == null ? null : check.timeout());
    }

    private static @Nullable String retriesDefault(InstanceDefaultsProperties defaults) {
        InstanceDefaultsProperties.CheckDefaults check = defaults.check();
        return number(check == null ? null : check.retries());
    }

    private static @Nullable String number(@Nullable Integer value) {
        return value == null ? null : String.valueOf(value);
    }
}
