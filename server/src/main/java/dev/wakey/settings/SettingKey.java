package dev.wakey.settings;

/** The stable keys of the settings catalog; plan bounds map onto some of them. */
public enum SettingKey {
    TIMEZONE("timezone"),
    LANGUAGE("language"),
    CHECK_INTERVAL("check.interval"),
    CHECK_TIMEOUT("check.timeout"),
    CHECK_RETRIES("check.retries");

    private final String key;

    SettingKey(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
