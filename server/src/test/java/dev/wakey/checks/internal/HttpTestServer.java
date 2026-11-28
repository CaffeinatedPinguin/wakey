package dev.wakey.checks.internal;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;

/** A local HTTP server that records how a check reached it. */
final class HttpTestServer implements AutoCloseable {

    private final HttpServer server;

    private final int status;

    private final AtomicInteger requests = new AtomicInteger();

    private final Set<Integer> remotePorts = ConcurrentHashMap.newKeySet();

    private final AtomicReference<@Nullable InetAddress> remoteAddress = new AtomicReference<>();

    private final AtomicReference<@Nullable String> hostHeader = new AtomicReference<>();

    HttpTestServer(int status) throws IOException {
        this.status = status;
        this.server = HttpServer.create(new InetSocketAddress(InetAddress.ofLiteral("127.0.0.1"), 0), 0);
        server.createContext("/", exchange -> {
            InetSocketAddress remote = exchange.getRemoteAddress();
            @Nullable String host = exchange.getRequestHeaders().getFirst("Host");
            requests.incrementAndGet();
            remotePorts.add(remote.getPort());
            remoteAddress.set(remote.getAddress());
            hostHeader.set(host);
            exchange.sendResponseHeaders(this.status, -1);
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

    Set<Integer> remotePorts() {
        return Set.copyOf(remotePorts);
    }

    @Nullable
    InetAddress remoteAddress() {
        return remoteAddress.get();
    }

    @Nullable
    String hostHeader() {
        return hostHeader.get();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
