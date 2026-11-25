package dev.wakey.spaces;

/** The authenticated caller the HTTP layer passes to the spaces module. */
public record SpacePrincipal(String subject, boolean admin) {

    public static final int MAX_SUBJECT_LENGTH = 100;
}
