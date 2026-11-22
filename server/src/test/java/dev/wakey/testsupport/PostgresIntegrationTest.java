package dev.wakey.testsupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for PostgreSQL integration tests.
 *
 * <p>The container is a singleton: it is started once when this class is loaded and lives until the
 * JVM exits, so Spring can cache one application context for every subclass. Do not use the
 * Testcontainers JUnit extension here; its per-class lifecycle stops the shared container after the
 * first test class. Subclasses add only their own tests: this class deliberately carries no test
 * cases of any feature.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class PostgresIntegrationTest {

    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

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
