package dev.wakey.checks.internal;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIMatcher;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.StandardConstants;
import org.jspecify.annotations.Nullable;

/** A local HTTPS server that records the SNI name and the Host header a check sends. */
final class HttpsTestServer implements AutoCloseable {

    private static final int SNI_HOST_NAME = StandardConstants.SNI_HOST_NAME;

    private final HttpsServer server;

    private final AtomicInteger requests = new AtomicInteger();

    private final AtomicReference<@Nullable String> sni = new AtomicReference<>();

    private final AtomicReference<@Nullable String> hostHeader = new AtomicReference<>();

    HttpsTestServer(int status) throws IOException {
        SSLContext sslContext = HttpTestTls.serverContext();
        this.server = HttpsServer.create(new InetSocketAddress(InetAddress.ofLiteral("127.0.0.1"), 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            @Override
            public void configure(HttpsParameters parameters) {
                SSLParameters sslParameters = sslContext.getDefaultSSLParameters();
                sslParameters.setSNIMatchers(List.of(new RecordingSniMatcher()));
                parameters.setSSLParameters(sslParameters);
            }
        });
        server.createContext("/", exchange -> {
            @Nullable String host = exchange.getRequestHeaders().getFirst("Host");
            requests.incrementAndGet();
            hostHeader.set(host);
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
    }

    int port() {
        return server.getAddress().getPort();
    }

    int requests() {
        return requests.get();
    }

    @Nullable
    String sni() {
        return sni.get();
    }

    @Nullable
    String hostHeader() {
        return hostHeader.get();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private final class RecordingSniMatcher extends SNIMatcher {

        private RecordingSniMatcher() {
            super(SNI_HOST_NAME);
        }

        @Override
        public boolean matches(SNIServerName name) {
            if (name instanceof SNIHostName hostName) {
                sni.set(hostName.getAsciiName());
            }
            return true;
        }
    }
}
