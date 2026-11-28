package dev.wakey.checks.internal;

import static org.assertj.core.api.Assertions.assertThat;

import dev.wakey.checks.Check;
import dev.wakey.checks.CheckResult;
import dev.wakey.checks.CheckState;
import dev.wakey.checks.ErrorCategory;
import java.io.IOException;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class HttpCheckExecutorTlsTest {

    @Test
    void anH2ServerIsCheckedOverHttp2() {
        // given
        try (H2TestServer server = new H2TestServer()) {
            HttpCheckExecutor executor = HttpCheckFixture.executor(address -> true, HttpTestTls.clientContext());
            Check check = HttpCheckFixture.httpsCheck("check.test", server.port());

            // when
            CheckResult result = executor.execute(check);

            // then
            assertThat(result.state()).isEqualTo(CheckState.UP);
            assertThat(result.httpStatus()).isEqualTo(200);
            assertThat(result.protocolVersion()).isEqualTo("HTTP/2");
        }
    }

    @Test
    void anHttp11OnlyServerFallsBackToHttp11() throws IOException {
        // given
        try (HttpsTestServer server = new HttpsTestServer(200)) {
            HttpCheckExecutor executor = HttpCheckFixture.executor(address -> true, HttpTestTls.clientContext());
            Check check = HttpCheckFixture.httpsCheck("check.test", server.port());

            // when
            CheckResult result = executor.execute(check);

            // then
            assertThat(result.state()).isEqualTo(CheckState.UP);
            assertThat(result.httpStatus()).isEqualTo(200);
            assertThat(result.protocolVersion()).isEqualTo("HTTP/1.1");
        }
    }

    @Test
    void aPinnedTlsConnectionKeepsSniAndVerifiesTheCertificate() throws IOException {
        // given
        try (HttpsTestServer server = new HttpsTestServer(200)) {
            int port = server.port();
            HttpCheckExecutor executor = HttpCheckFixture.executor(address -> true, HttpTestTls.clientContext());

            // when
            CheckResult result = executor.execute(HttpCheckFixture.httpsCheck("check.test", port));

            // then
            assertThat(result.state()).isEqualTo(CheckState.UP);
            assertThat(result.httpStatus()).isEqualTo(200);
            assertThat(server.sni()).isEqualTo("check.test");
            assertThat(server.hostHeader()).isEqualTo("check.test:" + port);
            assertThat(tlsPhaseRecorded(result)).isTrue();
        }
    }

    @Test
    void aCertificateForAnotherNameIsRejected() throws IOException {
        // given
        try (HttpsTestServer server = new HttpsTestServer(200)) {
            HttpCheckExecutor executor = HttpCheckFixture.executor(address -> true, HttpTestTls.clientContext());
            Check check = HttpCheckFixture.httpsCheck("other.test", server.port());

            // when
            CheckResult result = executor.execute(check);

            // then
            assertThat(result.state()).isEqualTo(CheckState.DOWN);
            assertThat(result.errorCategory()).isEqualTo(ErrorCategory.TARGET_DOWN);
            assertThat(result.httpStatus()).isNull();
            assertThat(server.sni()).isEqualTo("other.test");
            assertThat(server.requests()).isZero();
        }
    }

    private static boolean tlsPhaseRecorded(CheckResult result) {
        String phasesJson = Objects.requireNonNull(result.phases());
        var phases = HttpCheckFixture.mapper().readTree(phasesJson);
        return phases.path("tlsMs").isNumber();
    }
}
