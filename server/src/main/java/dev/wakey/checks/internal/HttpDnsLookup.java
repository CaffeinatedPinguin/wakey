package dev.wakey.checks.internal;

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.resolver.ResolvedAddressTypes;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.util.concurrent.Future;
import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The production lookup: a Netty DNS resolver on its own event loop, asking for A and AAAA records
 * once per check and honouring the check budget as the query timeout.
 */
final class HttpDnsLookup implements AutoCloseable {

    private static final int DNS_THREADS = 1;

    private final EventLoopGroup eventLoops = new MultiThreadIoEventLoopGroup(DNS_THREADS, NioIoHandler.newFactory());

    List<InetAddress> resolve(String host, Duration timeout) {
        long timeoutMillis = Math.max(1L, timeout.toMillis());
        EventLoop eventLoop = eventLoops.next();
        DnsNameResolver resolver = new DnsNameResolverBuilder(eventLoop)
                .datagramChannelType(NioDatagramChannel.class)
                .resolvedAddressTypes(ResolvedAddressTypes.IPV4_PREFERRED)
                .queryTimeoutMillis(timeoutMillis)
                .build();
        try {
            Future<List<InetAddress>> future = resolver.resolveAll(host);
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("DNS lookup interrupted for " + host, exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("DNS lookup failed for " + host, exception);
        } finally {
            resolver.close();
        }
    }

    @Override
    public void close() {
        eventLoops.shutdownGracefully().syncUninterruptibly();
    }
}
