package dev.wakey.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PostgresConnectionLimitTest extends PostgresIntegrationTest {

    private static final int POSTGRES_DEFAULT_MAX_CONNECTIONS = 100;

    @Test
    void theTestDatabaseKeepsItsDefaultConnectionLimit() {
        // when
        String maxConnections =
                jdbc().sql("SHOW max_connections").query(String.class).single();
        int configured = Integer.parseInt(maxConnections);

        // then
        assertThat(configured).isEqualTo(POSTGRES_DEFAULT_MAX_CONNECTIONS);
    }
}
