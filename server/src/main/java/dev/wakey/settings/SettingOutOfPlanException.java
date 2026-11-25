package dev.wakey.settings;

/** Rejects a value outside the plan bound; the bound and the plan travel with the failure. */
public final class SettingOutOfPlanException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String key;

    private final long value;

    private final long bound;

    private final String planName;

    public SettingOutOfPlanException(String key, long value, long bound, String planName) {
        super("Value " + value + " for setting " + key + " is outside the bound " + bound + " of plan " + planName);
        this.key = key;
        this.value = value;
        this.bound = bound;
        this.planName = planName;
    }

    public String key() {
        return key;
    }

    public long value() {
        return value;
    }

    public long bound() {
        return bound;
    }

    public String planName() {
        return planName;
    }
}
