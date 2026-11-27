package dev.wakey.audit.internal;

import dev.wakey.audit.ActorKind;
import dev.wakey.audit.AuditOutcome;
import dev.wakey.audit.AuditSource;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** One append-only audit event row; the database rejects updates and deletes. */
@Table("audit_event")
public record AuditEventEntity(
        @Id UUID id,
        Instant occurredAt,
        @Nullable String actorSubject,
        ActorKind actorKind,
        AuditSource source,
        @Nullable UUID spaceId,
        String action,
        String objectType,
        String objectId,
        AuditOutcome outcome,
        String details) {}
