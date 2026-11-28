package dev.wakey.checks.internal;

import static org.assertj.core.api.Assertions.assertThat;

import dev.wakey.checks.CheckResult;
import java.io.IOException;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class HttpCheckExecutorConnectionTest {

    @Test
    void theConnectionUsesTheResolvedIpAndKeepsTheHostHeader() throws IOException {
        // given
        try (HttpTestServer server = new HttpTestServer(200)) {
            HttpCheckExecutor executor = HttpCheckFixture.executor(address -> true);
            int port = server.port();
            String expectedHost = HttpCheckFixture.HOST + ":" + port;

            // when
            executor.execute(HttpCheckFixture.check(port));

            // then
            assertThat(server.hostHeader()).isEqualTo(expectedHost);
            assertThat(server.remoteAddress()).isEqualTo(HttpCheckFixture.LOOPBACK);
        }
    }

    @Test
    void everyCheckOpensANewConnection() throws IOException {
        // given
        try (HttpTestServer server = new HttpTestServer(200)) {
            HttpCheckExecutor executor = HttpCheckFixture.executor(address -> true);
            int port = server.port();

            // when
            executor.execute(HttpCheckFixture.check(port));
            executor.execute(HttpCheckFixture.check(port));

            // then
            assertThat(server.remotePorts()).hasSize(2);
        }
    }

    @Test
    void phasesAndProtocolVersionAreRecorded() throws IOException {
        // given
        try (HttpTestServer server = new HttpTestServer(200)) {
            HttpCheckExecutor executor = HttpCheckFixture.executor(address -> true);
            int port = server.port();

            // when
            CheckResult result = executor.execute(HttpCheckFixture.check(port));
            String phasesJson = Objects.requireNonNull(result.phases());
            var phases = HttpCheckFixture.mapper().readTree(phasesJson);
            boolean dnsRecorded = phases.path("dnsMs").isNumber();
            boolean connectRecorded = phases.path("connectMs").isNumber();
            boolean firstByteRecorded = phases.path("firstByteMs").isNumber();
            boolean totalRecorded = phases.path("totalMs").isNumber();

            // then
            assertThat(result.protocolVersion()).isEqualTo("HTTP/1.1");
            assertThat(dnsRecorded).isTrue();
            assertThat(connectRecorded).isTrue();
            assertThat(firstByteRecorded).isTrue();
            assertThat(totalRecorded).isTrue();
        }
    }
}
