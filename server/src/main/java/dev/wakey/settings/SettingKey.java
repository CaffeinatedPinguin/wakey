package dev.wakey.settings;

/** A stable key of the settings catalog; each module owns and contributes the keys it defines. */
public record SettingKey(String key) {}
