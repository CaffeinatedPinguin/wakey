package dev.wakey.checks;

/** The character bounds of the editable check fields, read by the request contract and its filters. */
public final class CheckFieldLimits {

    public static final int MIN_TEXT_LENGTH = 1;

    public static final int MAX_NAME_LENGTH = 100;

    public static final int MAX_TARGET_LENGTH = 2048;

    public static final int MAX_GROUP_LENGTH = 100;

    public static final int MAX_DESCRIPTION_LENGTH = 1000;

    public static final int MAX_RUNBOOK_LENGTH = 2048;

    public static final int MAX_TAGS = 20;

    public static final int MAX_TAG_LENGTH = 50;

    private CheckFieldLimits() {}
}
