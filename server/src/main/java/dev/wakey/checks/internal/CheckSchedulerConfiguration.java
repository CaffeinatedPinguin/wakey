package dev.wakey.checks.internal;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Binds the scheduler switches and creates the bounded task runner from the parallelism value. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CheckSchedulerProperties.class)
class CheckSchedulerConfiguration {

    @Bean(destroyMethod = "close")
    CheckSchedulerTasks checkSchedulerTasks(CheckSchedulerProperties properties) {
        return new CheckSchedulerTasks(properties.parallelism());
    }
}
