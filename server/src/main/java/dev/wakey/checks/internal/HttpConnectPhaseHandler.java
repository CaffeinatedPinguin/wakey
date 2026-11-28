package dev.wakey.checks.internal;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/** Marks the end of the TCP connection before TLS starts or the request bytes flow. */
final class HttpConnectPhaseHandler extends ChannelInboundHandlerAdapter {

    private final HttpCheckPhases phases;

    HttpConnectPhaseHandler(HttpCheckPhases phases) {
        this.phases = phases;
    }

    @Override
    public void channelActive(ChannelHandlerContext context) {
        phases.connected();
        context.fireChannelActive();
    }
}
