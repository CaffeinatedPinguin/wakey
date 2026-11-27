package dev.wakey.checks;

import dev.wakey.audit.ActorKind;
import dev.wakey.audit.AuditActor;
import dev.wakey.audit.AuditEntry;
import dev.wakey.audit.AuditLog;
import dev.wakey.audit.AuditOutcome;
import dev.wakey.audit.AuditSource;
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

    void created(CheckActor actor, UUID spaceId, UUID id) {
        String objectId = id.toString();
        var details = new CheckChange("check.create", CheckAuditDetails.created());
        record(actor, spaceId, new CheckTarget(OBJECT_TYPE, objectId), details);
    }

    void updated(CheckActor actor, UUID spaceId, Check existing, CheckDraft changes) {
        String objectId = existing.id().toString();
        String details = CheckAuditDetails.updated(existing, changes);
        record(actor, spaceId, new CheckTarget(OBJECT_TYPE, objectId), new CheckChange("check.update", details));
    }

    void paused(CheckActor actor, UUID spaceId, UUID id, PausedReason reason) {
        String objectId = id.toString();
        String details = CheckAuditDetails.paused(reason);
        record(actor, spaceId, new CheckTarget(OBJECT_TYPE, objectId), new CheckChange("check.pause", details));
    }

    void resumed(CheckActor actor, UUID spaceId, UUID id) {
        String objectId = id.toString();
        var details = new CheckChange("check.resume", CheckAuditDetails.resumed());
        record(actor, spaceId, new CheckTarget(OBJECT_TYPE, objectId), details);
    }

    void deleted(CheckActor actor, UUID spaceId, UUID id) {
        String objectId = id.toString();
        var details = new CheckChange("check.delete", CheckAuditDetails.deleted());
        record(actor, spaceId, new CheckTarget(OBJECT_TYPE, objectId), details);
    }

    void bulk(CheckActor actor, UUID spaceId, CheckBulkAction action, int affected) {
        String objectId = spaceId.toString();
        String details = CheckAuditDetails.bulk(action, affected);
        record(actor, spaceId, new CheckTarget(SPACE_OBJECT_TYPE, objectId), new CheckChange("check.bulk", details));
    }

    private void record(CheckActor actor, UUID spaceId, CheckTarget target, CheckChange change) {
        ActorKind kind = actor.token() ? ActorKind.TOKEN : ActorKind.USER;
        AuditActor auditActor = new AuditActor(actor.subject(), kind);
        AuditEntry entry = new AuditEntry(
                change.action(),
                target.objectType(),
                target.objectId(),
                AuditOutcome.SUCCESS,
                AuditSource.API,
                auditActor,
                spaceId,
                change.details());
        auditLog.record(entry);
    }

    private record CheckTarget(String objectType, String objectId) {}
}
