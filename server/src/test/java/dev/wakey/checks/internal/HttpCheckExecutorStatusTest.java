package dev.wakey.checks.internal;

import static org.assertj.core.api.Assertions.assertThat;

import dev.wakey.checks.CheckResult;
import dev.wakey.checks.CheckState;
import dev.wakey.checks.ErrorCategory;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class HttpCheckExecutorStatusTest {

    @Test
    void aSuccessfulResponseIsUp() throws IOException {
        // given
        try (HttpTestServer server = new HttpTestServer(200)) {
            HttpCheckExecutor executor = HttpCheckFixture.executor(address -> true);
            int port = server.port();

            // when
            CheckResult result = executor.execute(HttpCheckFixture.check(port));

            // then
            assertThat(result.state()).isEqualTo(CheckState.UP);
            assertThat(result.httpStatus()).isEqualTo(200);
            assertThat(result.errorCategory()).isNull();
            assertThat(result.durationMs()).isNotNull();
        }
    }

    @Test
    void aServerErrorIsDown() throws IOException {
        // given
        try (HttpTestServer server = new HttpTestServer(500)) {
            HttpCheckExecutor executor = HttpCheckFixture.executor(address -> true);
            int port = server.port();

            // when
            CheckResult result = executor.execute(HttpCheckFixture.check(port));

            // then
            assertThat(result.state()).isEqualTo(CheckState.DOWN);
            assertThat(result.httpStatus()).isEqualTo(500);
            assertThat(result.errorCategory()).isEqualTo(ErrorCategory.TARGET_DOWN);
        }
    }

    @Test
    void anUnreachableTargetIsDownWithItsCategory() throws IOException {
        // given
        int port = HttpCheckFixture.unusedPort();
        HttpCheckExecutor executor = HttpCheckFixture.executor(address -> true);

        // when
        CheckResult result = executor.execute(HttpCheckFixture.check(port));

        // then
        assertThat(result.state()).isEqualTo(CheckState.DOWN);
        assertThat(result.errorCategory()).isEqualTo(ErrorCategory.TARGET_DOWN);
        assertThat(result.httpStatus()).isNull();
    }

    @Test
    void aBlockedAddressIsNeverConnected() throws IOException {
        // given
        try (HttpTestServer server = new HttpTestServer(200)) {
            HttpCheckExecutor executor = HttpCheckFixture.executor(address -> false);
            int port = server.port();

            // when
            CheckResult result = executor.execute(HttpCheckFixture.check(port));

            // then
            assertThat(result.state()).isEqualTo(CheckState.DOWN);
            assertThat(result.errorCategory()).isEqualTo(ErrorCategory.TARGET_DOWN);
            assertThat(server.requests()).isZero();
        }
    }
}
