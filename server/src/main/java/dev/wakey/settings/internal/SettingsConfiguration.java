package dev.wakey.settings.internal;

import dev.wakey.settings.SettingCatalog;
import dev.wakey.settings.SettingDefinitionProvider;
import dev.wakey.settings.SettingError;
import dev.wakey.web.ErrorCodeSet;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Publishes the settings error codes to the API contract and collects every module's settings. */
@Configuration(proxyBeanMethods = false)
class SettingsConfiguration {

    @Bean
    ErrorCodeSet settingErrorCodes() {
        List<SettingError> codes = List.of(SettingError.values());
        return new ErrorCodeSet(codes);
    }

    @Bean
    SettingCatalog settingCatalog(List<SettingDefinitionProvider> providers) {
        return new SettingCatalog(providers);
    }
}
