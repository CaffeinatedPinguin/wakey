package dev.wakey.checks.internal;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import org.jspecify.annotations.Nullable;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;

/**
 * One HTTP exchange on a fresh connection to the pinned address, with phase observations. HTTPS
 * targets negotiate HTTP/2 or HTTP/1.1 over ALPN and use the injected TLS context, which keeps the
 * URI host for SNI and certificate checks.
 */
final class HttpCheckCall {

    private final SslContext sslContext;

    HttpCheckCall(SslContext sslContext) {
        this.sslContext = sslContext;
    }

    Result execute(URI target, InetSocketAddress pinned, Duration timeout, HttpCheckPhases phases) {
        HttpClient client = HttpClient.newConnection()
                .resolver(new HttpPinnedAddressResolverGroup(pinned))
                .doOnChannelInit((observer, channel, remoteAddress) ->
                        channel.pipeline().addFirst(new HttpConnectPhaseHandler(phases)))
                .doOnResponse((response, connection) -> phases.firstByte());
        HttpClient prepared = isSecure(target) ? secure(client, phases) : client;
        var future = prepared.get().uri(target).response().timeout(timeout).toFuture();
        @Nullable HttpClientResponse response;
        try {
            response = future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw new IllegalStateException("HTTP check interrupted", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("HTTP check failed", exception);
        }
        if (response == null) {
            throw new IllegalStateException("HTTP check finished without a response");
        }
        int status = response.status().code();
        String protocolVersion = protocolVersion(response);
        return new Result(status, protocolVersion);
    }

    private HttpClient secure(HttpClient client, HttpCheckPhases phases) {
        return client.protocol(HttpProtocol.H2, HttpProtocol.HTTP11)
                .secure(spec -> spec.sslContext(sslContext).handlerConfigurator(handler -> markTls(handler, phases)));
    }

    private static String protocolVersion(HttpClientResponse response) {
        String text = response.version().text();
        return text.startsWith("HTTP/2") ? "HTTP/2" : text;
    }

    private static void markTls(SslHandler handler, HttpCheckPhases phases) {
        handler.handshakeFuture().addListener(future -> {
            if (future.isSuccess()) {
                phases.tlsFinished();
            }
        });
    }

    private static boolean isSecure(URI target) {
        return "https".equalsIgnoreCase(target.getScheme());
    }

    /** The status and negotiated protocol of one completed exchange. */
    record Result(int status, String protocolVersion) {}
}
