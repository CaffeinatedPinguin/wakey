package dev.wakey.audit;

import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Append-only audit log; it is also the change history of a single object. */
public interface AuditLog {

    /**
     * Appends one entry as part of the caller's transaction, so the entry exists only when that
     * transaction commits. It requires an active transaction.
     *
     * @param entry the event to append
     */
    void record(AuditEntry entry);

    /**
     * Returns the history of one object in one space, newest first. A null space reads only the
     * entries that belong to no space, so one space never sees another space's history.
     *
     * @param spaceId the space that owns the object, or null for entries without a space
     * @param objectType the object kind, for example {@code space}
     * @param objectId the object identifier
     * @param page the cursor to resume after and the maximum number of entries
     * @return the next entries of the object, newest first
     */
    List<AuditRecord> history(@Nullable UUID spaceId, String objectType, String objectId, AuditPage page);
}
