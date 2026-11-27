package dev.wakey.checks;

import dev.wakey.settings.BoundDirection;
import dev.wakey.settings.PlanProperties;
import dev.wakey.settings.SettingDefinition;
import dev.wakey.settings.SettingDefinitionProvider;
import dev.wakey.settings.SettingGroup;
import dev.wakey.settings.SettingKey;
import dev.wakey.settings.SettingType;
import dev.wakey.web.ApiException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** The check settings and their rules; the checks module owns the settings a check reads. */
@Component
@RequiredArgsConstructor
public class CheckSettingDefinitions implements SettingDefinitionProvider {

    private static final String SECONDS = "s";

    private final CheckDefaultsProperties defaults;

    @Override
    public List<SettingDefinition> definitions() {
        return List.of(intervalDefinition(), timeoutDefinition(), retriesDefinition());
    }

    private SettingDefinition intervalDefinition() {
        return new SettingDefinition(
                CheckSettingKeys.INTERVAL,
                SettingGroup.DEFAULTS,
                SettingType.INTEGER,
                SECONDS,
                null,
                (long) CheckLimits.MIN_INTERVAL_SECONDS,
                null,
                value -> atLeast(value, CheckLimits.MIN_INTERVAL_SECONDS),
                BoundDirection.MIN,
                PlanProperties::minInterval,
                this::intervalValue,
                CheckSettingDefinitions::timeoutWithinInterval);
    }

    private SettingDefinition timeoutDefinition() {
        return new SettingDefinition(
                CheckSettingKeys.TIMEOUT,
                SettingGroup.DEFAULTS,
                SettingType.INTEGER,
                SECONDS,
                null,
                (long) CheckLimits.MIN_TIMEOUT_SECONDS,
                null,
                value -> atLeast(value, CheckLimits.MIN_TIMEOUT_SECONDS),
                BoundDirection.MAX,
                PlanProperties::maxTimeout,
                this::timeoutValue,
                CheckSettingDefinitions::timeoutWithinInterval);
    }

    private SettingDefinition retriesDefinition() {
        return new SettingDefinition(
                CheckSettingKeys.RETRIES,
                SettingGroup.DEFAULTS,
                SettingType.INTEGER,
                null,
                null,
                (long) CheckLimits.MIN_RETRIES,
                (long) CheckLimits.MAX_RETRIES,
                value -> between(value, CheckLimits.MIN_RETRIES, CheckLimits.MAX_RETRIES),
                BoundDirection.MAX,
                PlanProperties::maxRetries,
                this::retriesValue,
                null);
    }

    private String intervalValue() {
        return String.valueOf(defaults.interval());
    }

    private String timeoutValue() {
        return String.valueOf(defaults.timeout());
    }

    private String retriesValue() {
        return String.valueOf(defaults.retries());
    }

    private static boolean atLeast(String value, int minimum) {
        return SettingType.INTEGER.number(value) >= minimum;
    }

    private static boolean between(String value, int minimum, int maximum) {
        long parsed = SettingType.INTEGER.number(value);
        return parsed >= minimum && parsed <= maximum;
    }

    private static void timeoutWithinInterval(Map<SettingKey, String> values) {
        String timeoutValue = Objects.requireNonNull(values.get(CheckSettingKeys.TIMEOUT));
        String intervalValue = Objects.requireNonNull(values.get(CheckSettingKeys.INTERVAL));
        long timeout = SettingType.INTEGER.number(timeoutValue);
        long interval = SettingType.INTEGER.number(intervalValue);
        if (!CheckLimits.timeoutWithinInterval(timeout, interval)) {
            throw ApiException.invalidField(CheckSettingError.TIMEOUT_EXCEEDS_INTERVAL, "/value");
        }
    }
}
