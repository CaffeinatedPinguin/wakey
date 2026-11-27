package dev.wakey.config;

import static org.assertj.core.api.Assertions.assertThat;

import dev.wakey.testsupport.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;

class PostgresSessionLimitsTest extends PostgresIntegrationTest {

    @Test
    void appliesTheConfiguredSessionLimitsToEveryPooledConnection() {
        // when
        String idleTimeout = show("idle_in_transaction_session_timeout");
        String lockTimeout = show("lock_timeout");

        // then
        assertThat(idleTimeout).isEqualTo("45s");
        assertThat(lockTimeout).isEqualTo("3s");
    }

    private String show(String setting) {
        return jdbc().sql("SHOW " + setting).query(String.class).single();
    }
}
