package dev.wakey.settings;

/** The caller that changes a setting; a token caller is recorded as a token, never as a user. */
public record SettingActor(String subject, boolean token) {}
