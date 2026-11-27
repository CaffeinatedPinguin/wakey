package dev.wakey.checks;

/**
 * The authenticated subject that changes a check; {@code token} says whether the call was made with
 * an API token, so the module records it in the audit log as a token action.
 */
public record CheckActor(String subject, boolean token) {

    public CheckActor(String subject) {
        this(subject, false);
    }

    /** Creates the actor of a write from the subject and how it was authenticated. */
    public static CheckActor of(String subject, boolean token) {
        return new CheckActor(subject, token);
    }
}
