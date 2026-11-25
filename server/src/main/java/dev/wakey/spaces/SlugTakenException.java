package dev.wakey.spaces;

/** The requested slug is already taken; the HTTP layer maps it to a 409 conflict. */
public final class SlugTakenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SlugTakenException(String slug) {
        super("Space " + slug + " already exists");
    }
}
