package dev.wakey.checks.internal;

import dev.wakey.checks.CheckResults;
import dev.wakey.checks.CheckService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration(proxyBeanMethods = false)
class ChecksConfiguration {

    @Bean
    CheckService checkService(JdbcClient jdbc) {
        return new JdbcCheckService(new CheckSql(jdbc));
    }

    @Bean
    CheckResults checkResults(JdbcClient jdbc) {
        return new JdbcCheckResults(jdbc);
    }
}
