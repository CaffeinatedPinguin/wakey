package dev.wakey.spaces;

/** The caller is a space member but the role does not allow the action; the HTTP layer maps it to 403. */
public final class SpaceAccessDeniedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SpaceAccessDeniedException(String message) {
        super(message);
    }
}
