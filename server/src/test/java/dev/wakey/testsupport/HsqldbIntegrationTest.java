package dev.wakey.testsupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Base class for HSQLDB integration tests.
 *
 * <p>The in-memory datasource is configured here once, so every subclass shares the same Spring
 * application context. Subclasses add only their own tests: this class deliberately carries no test
 * cases of any feature.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.datasource.url=jdbc:hsqldb:mem:wakey-integration-tests",
            "spring.datasource.username=sa",
            "spring.datasource.password="
        })
public abstract class HsqldbIntegrationTest {

    private JdbcClient jdbc;

    @Autowired
    void setJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Shared accessor for subclasses and for the lease test contract. */
    public JdbcClient jdbc() {
        return jdbc;
    }
}
