package dev.wakey;

import static org.assertj.core.api.Assertions.assertThat;

import dev.wakey.testsupport.PostgresIntegrationTest;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

class PostgresStartupTest extends PostgresIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void contextStartsWithLiquibaseOnPostgresAndHealthIsUp() {
        // when
        Integer changelogTables = jdbc().sql(
                        "select count(*) from information_schema.tables where upper(table_name) = 'DATABASECHANGELOG'")
                .query(Integer.class)
                .single();
        ResponseEntity<String> response = RestClient.create("http://localhost:" + port)
                .get()
                .uri("/healthz")
                .retrieve()
                .toEntity(String.class);
        String body = Objects.requireNonNull(response.getBody());

        // then
        assertThat(changelogTables).isEqualTo(1);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body).contains("\"status\":\"UP\"");
    }
}
