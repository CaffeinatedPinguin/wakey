package dev.wakey.checks;

/** Rejects a check value outside the assigned plan; the bound and the plan travel with the failure. */
public final class CheckOutOfPlanException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String key;

    private final long bound;

    private final String planName;

    public CheckOutOfPlanException(String key, long bound, String planName) {
        super("A check value for " + key + " is outside the bound " + bound + " of plan " + planName);
        this.key = key;
        this.bound = bound;
        this.planName = planName;
    }

    public String key() {
        return key;
    }

    public long bound() {
        return bound;
    }

    public String planName() {
        return planName;
    }
}
