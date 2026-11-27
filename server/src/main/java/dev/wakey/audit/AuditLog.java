package dev.wakey.audit;

import dev.wakey.audit.internal.AuditStore;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Append-only audit log; it is also the change history of a single object. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLog {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final AuditStore store;

    private final Clock clock;

    /**
     * Appends one successful API write as part of the caller's transaction. It is the single write
     * path of the audit log: it serializes the details once and builds the entry.
     *
     * @param change the write to append
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuditChange change) {
        String details = JSON.writeValueAsString(change.details());
        AuditEntry entry = new AuditEntry(
                change.action(),
                change.objectType(),
                change.objectId(),
                AuditOutcome.SUCCESS,
                AuditSource.API,
                change.actor(),
                change.spaceId(),
                details);
        append(entry);
    }

    /**
     * Appends one entry as part of the caller's transaction, so the entry exists only when that
     * transaction commits. It requires an active transaction.
     *
     * @param entry the event to append
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuditEntry entry) {
        append(entry);
    }

    /**
     * Appends one event in its own transaction. Use it for events that are not part of a business
     * write, such as a login or a rejected request.
     *
     * @param entry the event to append
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSecurityEvent(AuditEntry entry) {
        append(entry);
    }

    private void append(AuditEntry entry) {
        UUID id = UUID.randomUUID();
        store.append(entry, id, clock.instant());
        log.debug("Recorded audit event {} for {} {}", entry.action(), entry.objectType(), entry.objectId());
    }

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
    public List<AuditRecord> history(@Nullable UUID spaceId, String objectType, String objectId, AuditPage page) {
        List<AuditRecord> records = store.history(spaceId, objectType, objectId, page);
        log.debug("Read {} audit events for {} {}", records.size(), objectType, objectId);
        return records;
    }
}
