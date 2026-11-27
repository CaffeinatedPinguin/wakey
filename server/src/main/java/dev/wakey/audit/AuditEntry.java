package dev.wakey.audit;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * One event to append to the audit log. {@code details} is JSON text built by the caller; the
 * module does not interpret it.
 */
public record AuditEntry(
        String action,
        String objectType,
        String objectId,
        AuditOutcome outcome,
        AuditSource source,
        AuditActor actor,
        @Nullable UUID spaceId,
        String details) {

    /** The longest accepted {@code details} payload, in characters. */
    public static final int MAX_DETAILS_LENGTH = 4000;

    public AuditEntry {
        if (details.length() > MAX_DETAILS_LENGTH) {
            throw new IllegalArgumentException(
                    "details must be at most " + MAX_DETAILS_LENGTH + " characters, but was " + details.length());
        }
    }
}
