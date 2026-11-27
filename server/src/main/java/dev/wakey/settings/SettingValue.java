package dev.wakey.settings;

/** The key and the raw value of one requested setting change. */
public record SettingValue(String key, String value) {}
