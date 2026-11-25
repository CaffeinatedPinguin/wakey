package dev.wakey.settings;

/** Rejects a plan name that is not configured on this instance. */
public final class UnknownPlanException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String planName;

    public UnknownPlanException(String planName) {
        super("Unknown plan: " + planName);
        this.planName = planName;
    }

    public String planName() {
        return planName;
    }
}
