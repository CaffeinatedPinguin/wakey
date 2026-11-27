package dev.wakey.audit;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** One stored audit event as read back from the log. */
public record AuditRecord(
        UUID id,
        Instant occurredAt,
        String action,
        String objectType,
        String objectId,
        AuditOutcome outcome,
        AuditSource source,
        AuditActor actor,
        @Nullable UUID spaceId,
        String details) {}
