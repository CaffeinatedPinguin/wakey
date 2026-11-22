package dev.wakey;

import static org.assertj.core.api.Assertions.assertThat;

import dev.wakey.testsupport.PostgresIntegrationTest;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

class HealthEndpointTest extends PostgresIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void healthEndpointIsUpOnHealthz() {
        // when
        ResponseEntity<String> response =
                client().get().uri("/healthz").retrieve().toEntity(String.class);
        String body = Objects.requireNonNull(response.getBody());

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body).contains("\"status\":\"UP\"");
    }

    @Test
    void actuatorEnvironmentEndpointIsNotExposed() {
        // when
        int actuatorStatus = statusOf("/actuator/env");
        int shortStatus = statusOf("/env");

        // then
        assertThat(actuatorStatus).isEqualTo(401);
        assertThat(shortStatus).isEqualTo(401);
    }

    private int statusOf(String path) {
        try {
            client().get().uri(path).retrieve().toBodilessEntity();
            return HttpStatus.OK.value();
        } catch (RestClientResponseException exception) {
            return exception.getStatusCode().value();
        }
    }

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }
}
