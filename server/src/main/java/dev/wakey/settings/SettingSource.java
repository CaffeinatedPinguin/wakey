package dev.wakey.settings;

/** Where an effective setting value comes from; the first source that defines it wins. */
public enum SettingSource {
    SPACE,
    PLAN,
    INSTANCE
}
