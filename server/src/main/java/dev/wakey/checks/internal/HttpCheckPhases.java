package dev.wakey.checks.internal;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.Nullable;

/** The phase marks of one check, recorded by the resolver, the client callbacks and the TLS handler. */
final class HttpCheckPhases {

    private static final long UNSET = -1L;

    private final long startedNanos = System.nanoTime();

    private final AtomicLong dnsNanos = new AtomicLong(UNSET);

    private final AtomicLong connectNanos = new AtomicLong(UNSET);

    private final AtomicLong tlsNanos = new AtomicLong(UNSET);

    private final AtomicLong firstByteNanos = new AtomicLong(UNSET);

    void dnsFinished() {
        mark(dnsNanos);
    }

    void connected() {
        mark(connectNanos);
    }

    void tlsFinished() {
        mark(tlsNanos);
    }

    void firstByte() {
        mark(firstByteNanos);
    }

    /** The total wall time of the check so far. */
    long totalMillis() {
        return elapsed(startedNanos, System.nanoTime());
    }

    Data data() {
        long dns = dnsNanos.get();
        long connect = connectNanos.get();
        long tls = tlsNanos.get();
        long firstByte = firstByteNanos.get();
        long firstByteFrom = tls == UNSET ? connect : tls;
        return new Data(
                phase(startedNanos, dns),
                phase(dns, connect),
                phase(connect, tls),
                phase(firstByteFrom, firstByte),
                totalMillis());
    }

    private static void mark(AtomicLong mark) {
        mark.compareAndSet(UNSET, System.nanoTime());
    }

    private static @Nullable Long phase(long from, long to) {
        if (from == UNSET || to == UNSET) {
            return null;
        }
        return elapsed(from, to);
    }

    private static long elapsed(long from, long to) {
        return TimeUnit.NANOSECONDS.toMillis(Math.max(0L, to - from));
    }

    /** The JSON shape of a check's phases, in milliseconds. */
    record Data(
            @Nullable Long dnsMs,
            @Nullable Long connectMs,
            @Nullable Long tlsMs,
            @Nullable Long firstByteMs,
            long totalMs) {}
}
