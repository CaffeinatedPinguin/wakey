package dev.wakey.audit;

import java.util.List;
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
     * Returns the history of one object, newest first, at most {@code limit} entries. Pass the
     * cursor of the last returned entry to resume after it.
     *
     * @param objectType the object kind, for example {@code space}
     * @param objectId the object identifier
     * @param after the last entry of the previous page, or {@code null} for the first page
     * @param limit the maximum number of entries to return
     * @return the next entries of the object, newest first
     */
    List<AuditRecord> history(String objectType, String objectId, @Nullable AuditCursor after, int limit);
}
