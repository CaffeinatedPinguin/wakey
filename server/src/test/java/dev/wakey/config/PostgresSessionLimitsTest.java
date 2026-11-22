package dev.wakey.config;

import static org.assertj.core.api.Assertions.assertThat;

import dev.wakey.testsupport.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {"wakey.database.idle-in-transaction-timeout=45s", "wakey.database.lock-timeout=3s"})
class PostgresSessionLimitsTest extends PostgresIntegrationTest {

    @Test
    void appliesTheConfiguredSessionLimitsToEveryPooledConnection() {
        assertThat(show("idle_in_transaction_session_timeout")).isEqualTo("45s");
        assertThat(show("lock_timeout")).isEqualTo("3s");
    }

    private String show(String setting) {
        return jdbc().sql("SHOW " + setting).query(String.class).single();
    }
}
