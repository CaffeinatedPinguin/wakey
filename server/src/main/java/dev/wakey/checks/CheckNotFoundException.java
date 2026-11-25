package dev.wakey.checks;

import java.util.UUID;

/** A check the caller cannot see; a missing check and a foreign one look the same (HTTP 404). */
public final class CheckNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CheckNotFoundException(UUID id) {
        super("Check " + id + " was not found");
    }
}
