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

    private static final String POSTGRESQL_JDBC_URL_PREFIX = "jdbc:postgresql:";

    static final String OPTIONS_TEMPLATE = "-c idle_in_transaction_session_timeout=%d -c lock_timeout=%d";

    @Bean
    static BeanPostProcessor postgresSessionLimits(ObjectProvider<DatabaseProperties> properties) {
        return new PostgresSessionLimitsBeanPostProcessor(properties);
    }

    @Bean
    ApplicationRunner postgresSessionLimitsStartupLog(DatabaseProperties properties, DataSource dataSource) {
        return unusedArguments -> logSessionLimits(properties, dataSource);
    }

    static boolean isPostgreSql(HikariDataSource dataSource) {
        String jdbcUrl = dataSource.getJdbcUrl();
        return jdbcUrl != null && jdbcUrl.startsWith(POSTGRESQL_JDBC_URL_PREFIX);
    }

    private static void logSessionLimits(DatabaseProperties properties, DataSource dataSource) {
        if (dataSource instanceof HikariDataSource hikari && isPostgreSql(hikari)) {
            Duration idleTimeout = properties.idleInTransactionTimeout();
            Duration lockTimeout = properties.lockTimeout();
            log.info(
                    "PostgreSQL session limits: idle_in_transaction_session_timeout={} ms, lock_timeout={} ms",
                    idleTimeout.toMillis(),
                    lockTimeout.toMillis());
        }
    }
}
