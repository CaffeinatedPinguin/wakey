package dev.wakey.checks.internal;

import dev.wakey.checks.CheckResult;
import dev.wakey.checks.CheckState;
import dev.wakey.checks.ErrorCategory;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Builds the stored result of a check from the HTTP exchange or from a failure. */
@Slf4j
final class HttpCheckOutcome {

    private static final int LAST_REDIRECT_STATUS = 399;

    private final ObjectMapper objectMapper;

    HttpCheckOutcome(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    CheckResult success(Start start, HttpCheckCall.Result exchange, HttpCheckPhases phases) {
        boolean up = exchange.status() >= 200 && exchange.status() <= LAST_REDIRECT_STATUS;
        CheckState state = up ? CheckState.UP : CheckState.DOWN;
        @Nullable ErrorCategory category = up ? null : ErrorCategory.TARGET_DOWN;
        String phasesJson = phasesJson(phases);
        int durationMs = durationMs(start.startedNanos());
        return new CheckResult(
                null,
                start.checkedAt(),
                state,
                category,
                exchange.status(),
                durationMs,
                exchange.protocolVersion(),
                phasesJson);
    }

    CheckResult failure(Start start, HttpCheckPhases phases) {
        String phasesJson = phasesJson(phases);
        int durationMs = durationMs(start.startedNanos());
        return new CheckResult(
                null,
                start.checkedAt(),
                CheckState.DOWN,
                ErrorCategory.TARGET_DOWN,
                null,
                durationMs,
                null,
                phasesJson);
    }

    private int durationMs(long startedNanos) {
        long millis = (System.nanoTime() - startedNanos) / 1_000_000L;
        return (int) Math.min(millis, Integer.MAX_VALUE);
    }

    private @Nullable String phasesJson(HttpCheckPhases phases) {
        try {
            return objectMapper.writeValueAsString(phases.data());
        } catch (JacksonException exception) {
            log.debug("Cannot serialize check phases", exception);
            return null;
        }
    }

    /** The moment and start mark of one check, shared by both result paths. */
    record Start(Instant checkedAt, long startedNanos) {}
}
