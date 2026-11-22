package dev.wakey.config;

import com.zaxxer.hikari.HikariDataSource;
import java.time.Duration;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures PostgreSQL session limits from {@code wakey.database.idle-in-transaction-timeout} and
 * {@code wakey.database.lock-timeout}. The values are passed to pgjdbc through the {@code options}
 * connection property: {@code -c idle_in_transaction_session_timeout=<ms> -c lock_timeout=<ms>}.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DatabaseProperties.class)
class DatabaseSessionLimitsConfiguration {

    // postgres-only: these `-c` options are PostgreSQL server settings passed through pgjdbc.
    static final String OPTIONS_TEMPLATE = "-c idle_in_transaction_session_timeout=%d -c lock_timeout=%d";

    @Bean
    static BeanPostProcessor postgresSessionLimits(ObjectProvider<DatabaseProperties> properties) {
        return new PostgresSessionLimitsBeanPostProcessor(properties);
    }

    @Bean
    ApplicationRunner postgresSessionLimitsStartupLog(DatabaseProperties properties, DataSource dataSource) {
        return unusedArguments -> logSessionLimits(properties, dataSource);
    }

    private static void logSessionLimits(DatabaseProperties properties, DataSource dataSource) {
        if (dataSource instanceof HikariDataSource) {
            Duration idleTimeout = properties.idleInTransactionTimeout();
            Duration lockTimeout = properties.lockTimeout();
            log.info(
                    "PostgreSQL session limits: idle_in_transaction_session_timeout={} ms, lock_timeout={} ms",
                    idleTimeout.toMillis(),
                    lockTimeout.toMillis());
        }
    }
}
