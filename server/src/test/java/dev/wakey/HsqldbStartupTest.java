package dev.wakey;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.datasource.url=jdbc:hsqldb:mem:wakey-hsqldb",
            "spring.datasource.username=sa",
            "spring.datasource.password="
        })
class HsqldbStartupTest {

    @LocalServerPort
    private int port;

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    HsqldbStartupTest(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void contextStartsWithLiquibaseOnHsqldbAndHealthIsUp() {
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
