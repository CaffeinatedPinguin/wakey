package dev.wakey.checks;

import dev.wakey.settings.SettingKey;

/**
 * The stable keys of the settings a check reads. A module owns its keys, so the settings catalog
 * does not need an enum entry for every module.
 */
public final class CheckSettingKeys {

    public static final SettingKey INTERVAL = new SettingKey("check.interval");

    public static final SettingKey TIMEOUT = new SettingKey("check.timeout");

    public static final SettingKey RETRIES = new SettingKey("check.retries");

    private CheckSettingKeys() {}
}
