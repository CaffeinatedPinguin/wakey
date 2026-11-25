package dev.wakey.settings;

/** Rejects an unknown setting key or a value that does not follow the catalog rule. */
public final class InvalidSettingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String key;

    private final String value;

    public InvalidSettingException(String key, String value, String message) {
        super(message);
        this.key = key;
        this.value = value;
    }

    public String key() {
        return key;
    }

    public String value() {
        return value;
    }
}
