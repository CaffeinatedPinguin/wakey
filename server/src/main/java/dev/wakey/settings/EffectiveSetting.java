package dev.wakey.settings;

/** The effective value of one setting together with the level it comes from. */
public record EffectiveSetting(String value, SettingSource source) {}
