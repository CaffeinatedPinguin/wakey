package dev.wakey.checks;

/** Rejects a new check when the space already reached the plan's maximum number of checks. */
public final class CheckPlanLimitException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final long bound;

    private final String planName;

    public CheckPlanLimitException(long bound, String planName) {
        super("The space reached the check limit " + bound + " of plan " + planName);
        this.bound = bound;
        this.planName = planName;
    }

    public long bound() {
        return bound;
    }

    public String planName() {
        return planName;
    }
}
