package dev.wakey.checks.internal;

import io.netty.resolver.AbstractAddressResolver;
import io.netty.resolver.AddressResolver;
import io.netty.resolver.AddressResolverGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * Resolves the one target of a check to the address that already passed the egress policy. The
 * client keeps the original URI host for the Host header, SNI and certificate verification, while
 * the socket connects only to the approved address.
 */
final class HttpPinnedAddressResolverGroup extends AddressResolverGroup<InetSocketAddress> {

    private final InetSocketAddress pinned;

    HttpPinnedAddressResolverGroup(InetSocketAddress pinned) {
        this.pinned = pinned;
    }

    @Override
    protected AddressResolver<InetSocketAddress> newResolver(EventExecutor executor) {
        return new PinnedAddressResolver(executor, pinned);
    }

    private static final class PinnedAddressResolver extends AbstractAddressResolver<InetSocketAddress> {

        private final InetSocketAddress pinned;

        private PinnedAddressResolver(EventExecutor executor, InetSocketAddress pinned) {
            super(executor);
            this.pinned = pinned;
        }

        @Override
        protected boolean doIsResolved(InetSocketAddress address) {
            return !address.isUnresolved();
        }

        @Override
        protected void doResolve(InetSocketAddress unresolvedAddress, Promise<InetSocketAddress> promise) {
            promise.setSuccess(pinned);
        }

        @Override
        protected void doResolveAll(InetSocketAddress unresolvedAddress, Promise<List<InetSocketAddress>> promise) {
            promise.setSuccess(List.of(pinned));
        }
    }
}
