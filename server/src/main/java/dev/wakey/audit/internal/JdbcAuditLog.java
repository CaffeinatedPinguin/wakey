package dev.wakey.audit.internal;

import dev.wakey.audit.ActorKind;
import dev.wakey.audit.AuditActor;
import dev.wakey.audit.AuditCursor;
import dev.wakey.audit.AuditEntry;
import dev.wakey.audit.AuditLog;
import dev.wakey.audit.AuditOutcome;
import dev.wakey.audit.AuditPage;
import dev.wakey.audit.AuditRecord;
import dev.wakey.audit.AuditSource;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
class JdbcAuditLog implements AuditLog {

    private static final RowMapper<AuditRecord> RECORD_MAPPER = (rs, unusedRowNum) -> {
        Instant occurredAt = rs.getTimestamp("occurred_at").toInstant();
        String actorSubject = rs.getString("actor_subject");
        ActorKind actorKind = ActorKind.valueOf(rs.getString("actor_kind"));
        AuditActor actor = new AuditActor(actorSubject, actorKind);
        AuditOutcome outcome = AuditOutcome.valueOf(rs.getString("outcome"));
        AuditSource source = AuditSource.valueOf(rs.getString("source"));
        @Nullable UUID spaceId = rs.getObject("space_id", UUID.class);
        return new AuditRecord(
                rs.getObject("id", UUID.class),
                occurredAt,
                rs.getString("action"),
                rs.getString("object_type"),
                rs.getString("object_id"),
                outcome,
                source,
                actor,
                spaceId,
                rs.getString("details"));
    };

    private static final String INSERT_ENTRY_SQL = """
            INSERT INTO audit_event (id, occurred_at, actor_subject, actor_kind, source, space_id,
                                     action, object_type, object_id, outcome, details)
            VALUES (:id, :occurredAt, :actorSubject, :actorKind, :source, :spaceId,
                    :action, :objectType, :objectId, :outcome, :details)
            """;

    private static final String HISTORY_SQL = """
            SELECT id, occurred_at, action, object_type, object_id, outcome, source,
                   actor_subject, actor_kind, space_id, details
            FROM audit_event
            WHERE space_id %s AND object_type = :objectType AND object_id = :objectId
            ORDER BY occurred_at DESC, id DESC
            LIMIT :rowLimit
            """;

    private static final String HISTORY_AFTER_SQL = """
            SELECT id, occurred_at, action, object_type, object_id, outcome, source,
                   actor_subject, actor_kind, space_id, details
            FROM audit_event
            WHERE space_id %s AND object_type = :objectType AND object_id = :objectId
              AND (occurred_at < :afterAt OR (occurred_at = :afterAt AND id < :afterId))
            ORDER BY occurred_at DESC, id DESC
            LIMIT :rowLimit
            """;

    private static final String SPACE_SCOPE = "= :spaceId";

    private static final String NO_SPACE_SCOPE = "IS NULL";

    private final JdbcClient jdbc;

    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuditEntry entry) {
        AuditActor actor = entry.actor();
        ActorKind actorKind = actor.kind();
        AuditSource source = entry.source();
        AuditOutcome outcome = entry.outcome();
        OffsetDateTime occurredAt = OffsetDateTime.now(clock);
        jdbc.sql(INSERT_ENTRY_SQL)
                .param("id", UUID.randomUUID())
                .param("occurredAt", occurredAt)
                .param("actorSubject", actor.subject())
                .param("actorKind", actorKind.name())
                .param("source", source.name())
                .param("spaceId", entry.spaceId())
                .param("action", entry.action())
                .param("objectType", entry.objectType())
                .param("objectId", entry.objectId())
                .param("outcome", outcome.name())
                .param("details", entry.details())
                .update();
        log.debug("Recorded audit event {} for {} {}", entry.action(), entry.objectType(), entry.objectId());
    }

    @Override
    public List<AuditRecord> history(@Nullable UUID spaceId, String objectType, String objectId, AuditPage page) {
        if (page.limit() <= 0) {
            log.debug("Read 0 audit events for {} {}", objectType, objectId);
            return List.of();
        }
        String scope = spaceId == null ? NO_SPACE_SCOPE : SPACE_SCOPE;
        String template = page.after() == null ? HISTORY_SQL : HISTORY_AFTER_SQL;
        JdbcClient.StatementSpec statement = jdbc.sql(String.format(template, scope))
                .param("objectType", objectType)
                .param("objectId", objectId)
                .param("rowLimit", page.limit());
        if (spaceId != null) {
            statement = statement.param("spaceId", spaceId);
        }
        if (page.after() != null) {
            statement = addCursor(statement, page.after());
        }
        List<AuditRecord> records = statement.query(RECORD_MAPPER).list();
        log.debug("Read {} audit events for {} {}", records.size(), objectType, objectId);
        return records;
    }

    private static JdbcClient.StatementSpec addCursor(JdbcClient.StatementSpec statement, AuditCursor after) {
        Instant occurredAt = after.occurredAt();
        OffsetDateTime afterAt = OffsetDateTime.ofInstant(occurredAt, ZoneOffset.UTC);
        return statement.param("afterAt", afterAt).param("afterId", after.id());
    }
}
