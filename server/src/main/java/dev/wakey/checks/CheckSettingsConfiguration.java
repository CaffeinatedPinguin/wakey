package dev.wakey.checks;

import dev.wakey.web.ErrorCodeSet;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Binds the check instance defaults and publishes the check setting codes to the API contract. The
 * codes stay in the module that owns them.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CheckDefaultsProperties.class)
public class CheckSettingsConfiguration {

    @Bean
    ErrorCodeSet checkSettingErrorCodes() {
        List<CheckSettingError> codes = List.of(CheckSettingError.values());
        return new ErrorCodeSet(codes);
    }
}
