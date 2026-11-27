package dev.wakey.checks.internal;

import dev.wakey.checks.Check;
import dev.wakey.checks.CheckState;
import dev.wakey.checks.CheckType;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import javax.net.ssl.SSLException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** Builds HTTP checks and executors that resolve the test host to loopback. */
final class HttpCheckFixture {

    static final String HOST = "checks.test";

    static final InetAddress LOOPBACK = InetAddress.ofLiteral("127.0.0.1");

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private static final SslContext DEFAULT_SSL_CONTEXT = defaultSslContext();

    private HttpCheckFixture() {}

    static HttpCheckExecutor executor(Predicate<InetAddress> policy) {
        return executor(policy, DEFAULT_SSL_CONTEXT);
    }

    static HttpCheckExecutor executor(Predicate<InetAddress> policy, SslContext sslContext) {
        HttpCheckOutcome outcome = new HttpCheckOutcome(MAPPER);
        HttpCheckEnvironment environment =
                new HttpCheckEnvironment(policy, (host, timeout) -> List.of(LOOPBACK), sslContext);
        return new HttpCheckExecutor(environment, Clock.systemUTC(), outcome);
    }

    static Check check(int port) {
        return check(target(port));
    }

    static Check check(String target) {
        return new Check(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "test",
                CheckType.HTTP,
                target,
                60,
                5,
                0,
                null,
                null,
                null,
                false,
                null,
                List.of(),
                CheckState.UNKNOWN,
                0L,
                Instant.EPOCH,
                Instant.EPOCH);
    }

    static Check httpsCheck(String host, int port) {
        return check("https://" + host + ":" + port + "/");
    }

    static String target(int port) {
        return "http://" + HOST + ":" + port + "/";
    }

    static int unusedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, LOOPBACK)) {
            return socket.getLocalPort();
        }
    }

    static ObjectMapper mapper() {
        return MAPPER;
    }

    private static SslContext defaultSslContext() {
        try {
            return SslContextBuilder.forClient()
                    .endpointIdentificationAlgorithm("HTTPS")
                    .build();
        } catch (SSLException exception) {
            throw new IllegalStateException("Cannot build the default test SSL context", exception);
        }
    }
}
