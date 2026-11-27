package dev.wakey.checks;

/**
 * Decides the effective state of a check from one result and whether the check still needs a
 * configuration review. A location network error never makes the target down, and a check without
 * any successful result stays marked for a configuration review.
 */
public final class CheckStatePolicy {

    private CheckStatePolicy() {}

    public static CheckOutcome evaluate(CheckResult result, boolean hasSuccessfulResult) {
        CheckState state = stateOf(result);
        boolean successful = isSuccessful(result);
        return new CheckOutcome(state, !hasSuccessfulResult && !successful);
    }

    /** The state this result contributes; a location network error can never prove the target down. */
    public static CheckState stateOf(CheckResult result) {
        if (result.errorCategory() == ErrorCategory.LOCATION_NETWORK) {
            return CheckState.UNKNOWN;
        }
        return result.state();
    }

    /**
     * A result is successful when the measurement itself worked: either the target answered
     * (including a target-down answer) or the check passed. Location and authentication failures
     * do not prove that the target is reachable.
     */
    public static boolean isSuccessful(CheckResult result) {
        return result.errorCategory() == null || result.errorCategory() == ErrorCategory.TARGET_DOWN;
    }
}
