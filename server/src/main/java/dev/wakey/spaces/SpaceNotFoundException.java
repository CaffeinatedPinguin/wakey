package dev.wakey.spaces;

/** A space the caller cannot see; a missing space and a foreign one look the same, so the HTTP layer maps it to 404. */
public final class SpaceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SpaceNotFoundException(String slug) {
        super("Space " + slug + " was not found");
    }
}
