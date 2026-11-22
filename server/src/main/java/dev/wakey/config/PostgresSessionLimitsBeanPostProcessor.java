package dev.wakey.config;

import com.zaxxer.hikari.HikariDataSource;
import java.time.Duration;
import java.util.Properties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;

final class PostgresSessionLimitsBeanPostProcessor implements BeanPostProcessor {

    // postgres-only: pgjdbc turns the `options` connection property into PostgreSQL server settings.
    private static final String OPTIONS_PROPERTY = "options";

    private final ObjectProvider<DatabaseProperties> properties;

    PostgresSessionLimitsBeanPostProcessor(ObjectProvider<DatabaseProperties> properties) {
        this.properties = properties;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof HikariDataSource dataSource) {
            applySessionLimits(dataSource);
        }
        return bean;
    }

    private void applySessionLimits(HikariDataSource dataSource) {
        DatabaseProperties databaseProperties = properties.getObject();
        Duration idleTimeout = databaseProperties.idleInTransactionTimeout();
        Duration lockTimeout = databaseProperties.lockTimeout();
        String options = DatabaseSessionLimitsConfiguration.OPTIONS_TEMPLATE.formatted(
                idleTimeout.toMillis(), lockTimeout.toMillis());
        dataSource.addDataSourceProperty(OPTIONS_PROPERTY, mergeWithExistingOptions(dataSource, options));
    }

    private static String mergeWithExistingOptions(HikariDataSource dataSource, String options) {
        Properties dataSourceProperties = dataSource.getDataSourceProperties();
        String existingOptions =
                dataSourceProperties == null ? "" : dataSourceProperties.getProperty(OPTIONS_PROPERTY, "");
        return existingOptions.isBlank() ? options : existingOptions + " " + options;
    }
}
