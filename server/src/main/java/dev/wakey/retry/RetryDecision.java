package dev.wakey.retry;

import java.time.Duration;

/** The outcome of one retry decision: wait and try again, or stop with a final reason. */
public sealed interface RetryDecision {

    /** The next attempt may start after the given delay. */
    record RetryAfter(Duration delay) implements RetryDecision {}

    /** No further attempts are allowed; the reason names the exhausted limit. */
    record GiveUp(Reason reason) implements RetryDecision {

        /** The limit that ended the retry sequence. */
        public enum Reason {
            MAX_ATTEMPTS,
            TIME_BUDGET
        }
    }
}
