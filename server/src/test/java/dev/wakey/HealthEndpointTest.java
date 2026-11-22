package dev.wakey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import dev.wakey.testsupport.HsqldbIntegrationTest;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

class HealthEndpointTest extends HsqldbIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void healthEndpointIsUpOnHealthz() {
        ResponseEntity<String> response =
                client().get().uri("/healthz").retrieve().toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(response.getBody())).contains("\"status\":\"UP\"");
    }

    @Test
    void actuatorEnvironmentEndpointIsNotExposed() {
        assertNotFound("/actuator/env");
        assertNotFound("/env");
    }

    private void assertNotFound(String path) {
        try {
            client().get().uri(path).retrieve().toBodilessEntity();
            fail("Expected a 404 response for " + path);
        } catch (RestClientResponseException exception) {
            assertThat(exception.getStatusCode().value()).isEqualTo(404);
        }
    }

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }
}
