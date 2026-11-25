package dev.wakey.checks;

import java.util.UUID;

/** An update carried a version other than the stored one; the HTTP layer maps it to a conflict. */
public final class CheckVersionConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final long expectedVersion;

    public CheckVersionConflictException(UUID id, long expectedVersion, long actualVersion) {
        super("Check " + id + " is at version " + actualVersion + ", not " + expectedVersion);
        this.expectedVersion = expectedVersion;
    }

    public long expectedVersion() {
        return expectedVersion;
    }
}
