package dev.wakey.checks;

import dev.wakey.settings.SettingError;
import dev.wakey.web.ErrorCode;
import dev.wakey.web.ErrorCodeSet;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Binds the check instance defaults and publishes the plan and check setting codes to the API
 * contract. The codes stay in the modules that own them; this list only fixes their public order.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({CheckDefaultsProperties.class, EgressProperties.class})
public class CheckSettingsConfiguration {

    @Bean
    ErrorCodeSet checkSettingErrorCodes() {
        List<ErrorCode> codes = new ArrayList<>();
        codes.add(SettingError.OUT_OF_PLAN);
        List<ErrorCode> checkCodes = List.of(CheckError.values());
        codes.addAll(checkCodes);
        codes.add(SettingError.UNKNOWN_PLAN);
        codes.add(SettingError.INVALID_KEY);
        codes.add(SettingError.INVALID_VALUE);
        List<ErrorCode> checkSettingCodes = List.of(CheckSettingError.values());
        codes.addAll(checkSettingCodes);
        return new ErrorCodeSet(codes);
    }
}
