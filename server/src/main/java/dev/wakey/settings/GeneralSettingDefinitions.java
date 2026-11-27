package dev.wakey.settings;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** The settings owned by the settings module itself: the time zone and the language. */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeneralSettingDefinitions implements SettingDefinitionProvider {

    private static final SettingKey TIMEZONE = new SettingKey("timezone");

    private static final SettingKey LANGUAGE = new SettingKey("language");

    private static final List<Language> LANGUAGES = List.of(Language.values());

    private final InstanceDefaultsProperties defaults;

    @Override
    public List<SettingDefinition> definitions() {
        return List.of(
                new SettingDefinition(
                        TIMEZONE,
                        SettingGroup.GENERAL,
                        SettingType.STRING,
                        null,
                        null,
                        null,
                        null,
                        GeneralSettingDefinitions::isTimeZone,
                        BoundDirection.MAX,
                        plan -> null,
                        defaults::timezone,
                        null),
                new SettingDefinition(
                        LANGUAGE,
                        SettingGroup.GENERAL,
                        SettingType.ENUM,
                        null,
                        LANGUAGES,
                        null,
                        null,
                        Language::isSupported,
                        BoundDirection.MAX,
                        plan -> null,
                        this::languageValue,
                        null));
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

    private String languageValue() {
        return defaults.language().code();
    }
}
