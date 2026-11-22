package dev.wakey;

import static org.assertj.core.api.Assertions.assertThat;

import dev.wakey.testsupport.HsqldbIntegrationTest;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

class HsqldbStartupTest extends HsqldbIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void contextStartsWithLiquibaseOnHsqldbAndHealthIsUp() {
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
