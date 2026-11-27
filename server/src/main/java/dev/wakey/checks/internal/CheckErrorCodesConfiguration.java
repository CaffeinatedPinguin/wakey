package dev.wakey.checks.internal;

import dev.wakey.checks.CheckError;
import dev.wakey.web.ErrorCodeSet;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class CheckErrorCodesConfiguration {

    @Bean
    ErrorCodeSet checkErrorCodes() {
        List<CheckError> codes = List.of(CheckError.values());
        return new ErrorCodeSet(codes);
    }
}
