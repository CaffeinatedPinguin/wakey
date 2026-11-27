package dev.wakey.audit.internal;

import dev.wakey.audit.AuditActor;
import dev.wakey.audit.AuditCursor;
import dev.wakey.audit.AuditEntry;
import dev.wakey.audit.AuditPage;
import dev.wakey.audit.AuditRecord;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Component;

/** The persistence adapter of the audit log: Spring Data JDBC writes and reads audit events. */
@Component
@RequiredArgsConstructor
public class AuditStore {

    private final JdbcAggregateTemplate aggregates;

    public void append(AuditEntry entry, UUID id, Instant occurredAt) {
        AuditActor actor = entry.actor();
        AuditEventEntity event = new AuditEventEntity(
                id,
                occurredAt,
                actor.subject(),
                actor.kind(),
                entry.source(),
                entry.spaceId(),
                entry.action(),
                entry.objectType(),
                entry.objectId(),
                entry.outcome(),
                entry.details());
        aggregates.insert(event);
    }

    public List<AuditRecord> history(@Nullable UUID spaceId, String objectType, String objectId, AuditPage page) {
        if (page.limit() <= 0) {
            return List.of();
        }
        Query query = historyQuery(spaceId, objectType, objectId, page);
        List<AuditEventEntity> events = aggregates.findAll(query, AuditEventEntity.class);
        return events.stream().map(AuditStore::toRecord).toList();
    }

    private static Query historyQuery(@Nullable UUID spaceId, String objectType, String objectId, AuditPage page) {
        Criteria criteria =
                scope(spaceId).and("objectType").is(objectType).and("objectId").is(objectId);
        @Nullable AuditCursor after = page.after();
        if (after != null) {
            criteria = criteria.and(afterCriteria(after));
        }
        Sort sort = Sort.by("occurredAt", "id").descending();
        return Query.query(criteria).sort(sort).limit(page.limit());
    }

    private static Criteria scope(@Nullable UUID spaceId) {
        if (spaceId == null) {
            return Criteria.where("spaceId").isNull();
        }
        return Criteria.where("spaceId").is(spaceId);
    }

    private static Criteria afterCriteria(AuditCursor cursor) {
        Criteria earlier = Criteria.where("occurredAt").lessThan(cursor.occurredAt());
        Criteria tie =
                Criteria.where("occurredAt").is(cursor.occurredAt()).and("id").lessThan(cursor.id());
        return earlier.or(tie);
    }

    private static AuditRecord toRecord(AuditEventEntity event) {
        AuditActor actor = new AuditActor(event.actorSubject(), event.actorKind());
        return new AuditRecord(
                event.id(),
                event.occurredAt(),
                event.action(),
                event.objectType(),
                event.objectId(),
                event.outcome(),
                event.source(),
                actor,
                event.spaceId(),
                event.details());
    }
}
