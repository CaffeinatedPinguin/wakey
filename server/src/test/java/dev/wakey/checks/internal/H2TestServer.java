package dev.wakey.checks.internal;

import reactor.netty.DisposableServer;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;

/** A local Reactor Netty HTTPS server that negotiates HTTP/2 or HTTP/1.1 over ALPN. */
final class H2TestServer implements AutoCloseable {

    private final DisposableServer server;

    H2TestServer() {
        this.server = HttpServer.create()
                .host("127.0.0.1")
                .port(0)
                .protocol(HttpProtocol.H2, HttpProtocol.HTTP11)
                .secure(spec -> spec.sslContext(HttpTestTls.nettyServerContext()))
                .handle((request, response) -> response.status(200).send())
                .bindNow();
    }

    int port() {
        return server.port();
    }

    @Override
    public void close() {
        server.disposeNow();
    }
}
