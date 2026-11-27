package dev.wakey.settings.internal;

import dev.wakey.settings.InstanceDefaultsProperties;
import dev.wakey.settings.PlanProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Binds the plan definitions and validates them once, when the application starts. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(InstanceDefaultsProperties.class)
class PlansConfiguration {

    @Bean
    @ConfigurationProperties("wakey.plans")
    Map<String, PlanProperties> plans() {
        return new LinkedHashMap<>();
    }

    @Bean
    PlanCatalog planCatalog(Map<String, PlanProperties> plans) {
        return PlanCatalog.from(plans);
    }
}
