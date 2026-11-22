package dev.wakey.testsupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Shared access to the Spring beans of an integration test context. */
public abstract class IntegrationTestSupport {

    private JdbcClient jdbc;

    private ApplicationContext applicationContext;

    @Autowired
    void setDependencies(JdbcClient jdbc, ApplicationContext applicationContext) {
        this.jdbc = jdbc;
        this.applicationContext = applicationContext;
    }

    public JdbcClient jdbc() {
        return jdbc;
    }

    public <T> T bean(Class<T> type) {
        return applicationContext.getBean(type);
    }
}
