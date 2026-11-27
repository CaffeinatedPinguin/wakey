package dev.wakey.testsupport;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL integration tests share one singleton container and the shared {@code test} profile.
 * A test that needs no extra configuration reuses the same Spring context, so the plans, the
 * instance defaults and the clock live in the profile and the base configuration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestClockConfiguration.class)
public abstract class PostgresIntegrationTest extends IntegrationTestSupport {

    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }
}
