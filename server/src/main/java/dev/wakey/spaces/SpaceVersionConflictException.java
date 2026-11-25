package dev.wakey.spaces;

/** The space changed since the version the caller sent; the HTTP layer maps it to a 409 conflict. */
public final class SpaceVersionConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SpaceVersionConflictException(String slug) {
        super("Space " + slug + " changed since the supplied version");
    }
}
