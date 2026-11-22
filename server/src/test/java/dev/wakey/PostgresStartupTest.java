package dev.wakey;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PostgresStartupTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @LocalServerPort
    private int port;

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    PostgresStartupTest(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void contextStartsWithLiquibaseOnPostgresAndHealthIsUp() {
        var changelogTables = jdbcTemplate.queryForList(
                "select count(*) from information_schema.tables" + " where upper(table_name) = 'DATABASECHANGELOG'",
                Integer.class);
        assertThat(changelogTables).containsExactly(1);

        ResponseEntity<String> response = RestClient.create("http://localhost:" + port)
                .get()
                .uri("/healthz")
                .retrieve()
                .toEntity(String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(response.getBody())).contains("\"status\":\"UP\"");
    }
}
