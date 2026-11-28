package dev.wakey.checks.internal;

import io.netty.handler.ssl.SslContext;
import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * The collaborators an HTTP check needs besides the clock: the egress address policy, the one-time
 * name lookup and the TLS context used for HTTPS targets.
 */
record HttpCheckEnvironment(
        Predicate<InetAddress> addressPolicy,
        BiFunction<String, Duration, List<InetAddress>> lookup,
        SslContext sslContext) {}
