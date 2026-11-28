package dev.wakey.checks;

import dev.wakey.audit.AuditActor;
import dev.wakey.audit.AuditChange;
import dev.wakey.audit.AuditLog;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Appends the audit entry of a check write in the caller's transaction. */
@Component
@RequiredArgsConstructor
public class CheckAuditLog {

    private static final String OBJECT_TYPE = "check";

    private static final String SPACE_OBJECT_TYPE = "space";

    private final AuditLog auditLog;

    void created(AuditActor actor, UUID spaceId, UUID id) {
        AuditChange change = new AuditChange(
                "check.create", OBJECT_TYPE, id.toString(), spaceId, actor, CheckAuditDetails.created());
        auditLog.record(change);
    }

    void updated(AuditActor actor, UUID spaceId, Check existing, CheckDraft changes) {
        String objectId = existing.id().toString();
        Map<String, Object> details = CheckAuditDetails.updated(existing, changes);
        AuditChange change = new AuditChange("check.update", OBJECT_TYPE, objectId, spaceId, actor, details);
        auditLog.record(change);
    }

    void paused(AuditActor actor, UUID spaceId, UUID id, PausedReason reason) {
        Map<String, Object> details = CheckAuditDetails.paused(reason);
        String objectId = id.toString();
        AuditChange change = new AuditChange("check.pause", OBJECT_TYPE, objectId, spaceId, actor, details);
        auditLog.record(change);
    }

    void resumed(AuditActor actor, UUID spaceId, UUID id) {
        String objectId = id.toString();
        AuditChange change =
                new AuditChange("check.resume", OBJECT_TYPE, objectId, spaceId, actor, CheckAuditDetails.resumed());
        auditLog.record(change);
    }

    void deleted(AuditActor actor, UUID spaceId, UUID id) {
        String objectId = id.toString();
        AuditChange change =
                new AuditChange("check.delete", OBJECT_TYPE, objectId, spaceId, actor, CheckAuditDetails.deleted());
        auditLog.record(change);
    }

    void bulk(AuditActor actor, UUID spaceId, CheckBulkAction action, int affected) {
        Map<String, Object> details = CheckAuditDetails.bulk(action, affected);
        String objectId = spaceId.toString();
        AuditChange change = new AuditChange("check.bulk", SPACE_OBJECT_TYPE, objectId, spaceId, actor, details);
        auditLog.record(change);
    }
}
