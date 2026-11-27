package dev.wakey.checks.internal;

import dev.wakey.checks.Check;
import dev.wakey.checks.CheckExecutor;
import dev.wakey.checks.CheckFieldLimits;
import dev.wakey.checks.CheckFormDescriptor;
import dev.wakey.checks.CheckResult;
import dev.wakey.checks.CheckType;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs one HTTP check: resolves the name once, validates every address against the injected egress
 * policy, pins the approved address and reports the status, protocol and phase timeline.
 */
@Slf4j
final class HttpCheckExecutor implements CheckExecutor {

    private static final long TIMEOUT_MIN_SECONDS = 1L;

    private static final int HTTPS_PORT = 443;

    private static final int HTTP_PORT = 80;

    private final HttpCheckEnvironment environment;

    private final Clock clock;

    private final HttpCheckOutcome outcome;

    HttpCheckExecutor(HttpCheckEnvironment environment, Clock clock, HttpCheckOutcome outcome) {
        this.environment = environment;
        this.clock = clock;
        this.outcome = outcome;
    }

    @Override
    public CheckType type() {
        return CheckType.HTTP;
    }

    @Override
    public CheckFormDescriptor formDescriptor() {
        return new CheckFormDescriptor(
                CheckType.HTTP,
                List.of(
                        new CheckFormDescriptor.Field(
                                "target",
                                CheckFormDescriptor.FieldType.URL,
                                true,
                                null,
                                (long) CheckFieldLimits.MAX_TARGET_LENGTH,
                                null),
                        new CheckFormDescriptor.Field(
                                "timeoutSeconds",
                                CheckFormDescriptor.FieldType.INTEGER,
                                true,
                                TIMEOUT_MIN_SECONDS,
                                null,
                                "s")));
    }

    @Override
    public CheckResult execute(Check check) {
        Instant checkedAt = clock.instant();
        long startedNanos = System.nanoTime();
        HttpCheckPhases phases = new HttpCheckPhases();
        HttpCheckOutcome.Start start = new HttpCheckOutcome.Start(checkedAt, startedNanos);
        try {
            return run(check, start, phases);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            log.debug("Check {} failed on target {}", check.id(), check.target(), exception);
            return outcome.failure(start, phases);
        }
    }

    private CheckResult run(Check check, HttpCheckOutcome.Start start, HttpCheckPhases phases) {
        URI target = URI.create(check.target());
        Duration budget = Duration.ofSeconds(check.timeoutSeconds());
        Duration dnsBudget = remaining(start.startedNanos(), budget);
        List<InetAddress> addresses = environment.lookup().apply(hostOf(target), dnsBudget);
        phases.dnsFinished();
        if (!allAllowed(check, addresses) || addresses.isEmpty()) {
            return outcome.failure(start, phases);
        }
        InetAddress address = addresses.get(0);
        InetSocketAddress pinned = new InetSocketAddress(address, portOf(target));
        Duration exchangeBudget = remaining(start.startedNanos(), budget);
        HttpCheckCall call = new HttpCheckCall(environment.sslContext());
        HttpCheckCall.Result exchange = call.execute(target, pinned, exchangeBudget, phases);
        CheckResult result = outcome.success(start, exchange, phases);
        log.debug(
                "Check {} finished with state {} and status {} in {} ms",
                check.id(),
                result.state(),
                result.httpStatus(),
                result.durationMs());
        return result;
    }

    private boolean allAllowed(Check check, List<InetAddress> addresses) {
        boolean allowed = true;
        for (InetAddress address : addresses) {
            if (!environment.addressPolicy().test(address)) {
                log.debug("Check {} refused blocked address {}", check.id(), address.getHostAddress());
                allowed = false;
            }
        }
        return allowed;
    }

    private static String hostOf(URI target) {
        String host = target.getHost();
        if (host == null) {
            throw new IllegalArgumentException("Check target has no host");
        }
        boolean bracketed = host.startsWith("[") && host.endsWith("]");
        return bracketed ? host.substring(1, host.length() - 1) : host;
    }

    private static int portOf(URI target) {
        int port = target.getPort();
        if (port >= 0) {
            return port;
        }
        boolean secure = "https".equalsIgnoreCase(target.getScheme());
        return secure ? HTTPS_PORT : HTTP_PORT;
    }

    private static Duration remaining(long startedNanos, Duration budget) {
        long leftNanos = budget.toNanos() - (System.nanoTime() - startedNanos);
        if (leftNanos <= 0) {
            throw new IllegalStateException("Check budget exhausted");
        }
        return Duration.ofNanos(leftNanos);
    }
}
