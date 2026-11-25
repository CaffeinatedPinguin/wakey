package dev.wakey.settings.internal;

import dev.wakey.settings.PlanCatalog;
import dev.wakey.settings.SpaceSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration(proxyBeanMethods = false)
class SettingsConfiguration {

    @Bean
    SpaceSettings spaceSettings(JdbcClient jdbc, PlanCatalog planCatalog) {
        return new JdbcSpaceSettings(jdbc, planCatalog);
    }
}
