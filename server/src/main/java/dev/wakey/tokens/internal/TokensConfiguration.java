package dev.wakey.tokens.internal;

import dev.wakey.tokens.TokenError;
import dev.wakey.web.ErrorCodeSet;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TokenProperties.class)
class TokensConfiguration {

    @Bean
    ErrorCodeSet tokenErrorCodes() {
        List<TokenError> codes = List.of(TokenError.values());
        return new ErrorCodeSet(codes);
    }
}
