package dev.wakey.testsupport;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Replaces the test data source with a wrapper that counts the statements a test runs. */
@TestConfiguration(proxyBeanMethods = false)
public class QueryCountingConfiguration {

    @Bean
    QueryCountingDataSource dataSource(JdbcConnectionDetails connectionDetails) {
        @Nullable String username = connectionDetails.getUsername();
        @Nullable String password = connectionDetails.getPassword();
        DriverManagerDataSource delegate = new DriverManagerDataSource();
        delegate.setDriverClassName(connectionDetails.getDriverClassName());
        delegate.setUrl(connectionDetails.getJdbcUrl());
        delegate.setUsername(username == null ? "" : username);
        delegate.setPassword(password == null ? "" : password);
        return new QueryCountingDataSource(delegate);
    }
}
