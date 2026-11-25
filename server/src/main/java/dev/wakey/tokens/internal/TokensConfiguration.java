package dev.wakey.tokens.internal;

import dev.wakey.spaces.SpaceService;
import dev.wakey.tokens.ApiTokens;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TokenProperties.class)
class TokensConfiguration {

    @Bean
    ApiTokens apiTokens(JdbcClient jdbc, SpaceService spaces, Clock clock, TokenProperties properties) {
        ApiTokenStore store = new ApiTokenStore(jdbc);
        SpaceAccess access = new SpaceAccess(spaces);
        return new JdbcApiTokens(store, access, clock, properties.maxPerOwner());
    }
}
