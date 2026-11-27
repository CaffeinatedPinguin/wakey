package dev.wakey.settings;

import dev.wakey.audit.ActorKind;
import dev.wakey.audit.AuditActor;
import dev.wakey.audit.AuditEntry;
import dev.wakey.audit.AuditLog;
import dev.wakey.audit.AuditOutcome;
import dev.wakey.audit.AuditSource;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/** Appends the settings audit events; details hold the old and the new value only. */
@Component
@RequiredArgsConstructor
public class SettingAudit {

    private static final String SETTING_OBJECT_TYPE = "space_setting";

    private static final String SPACE_OBJECT_TYPE = "space";

    private static final String PLAN_ACTION = "space.plan";

    private final AuditLog auditLog;

    void settingChanged(SettingChange change) {
        String details = changeDetails(change.oldValue(), change.newValue());
        Subject subject = new Subject(SETTING_OBJECT_TYPE, change.key(), change.spaceId());
        append(change.action(), subject, change.actor(), details);
    }

    void planChanged(PlanChange change) {
        String details = changeDetails(change.oldPlan(), change.newPlan());
        Subject subject = new Subject(SPACE_OBJECT_TYPE, change.slug(), change.spaceId());
        append(PLAN_ACTION, subject, change.actor(), details);
    }

    private void append(String action, Subject subject, SettingActor actor, String details) {
        ActorKind kind = actor.token() ? ActorKind.TOKEN : ActorKind.USER;
        AuditActor auditActor = new AuditActor(actor.subject(), kind);
        AuditEntry entry = new AuditEntry(
                action,
                subject.objectType(),
                subject.objectId(),
                AuditOutcome.SUCCESS,
                AuditSource.API,
                auditActor,
                subject.spaceId(),
                details);
        auditLog.record(entry);
    }

    private record Subject(String objectType, String objectId, UUID spaceId) {}

    private static String changeDetails(@Nullable String oldValue, @Nullable String newValue) {
        return "{\"old\":" + jsonValue(oldValue) + ",\"new\":" + jsonValue(newValue) + "}";
    }

    private static String jsonValue(@Nullable String value) {
        return value == null ? "null" : "\"" + escape(value) + "\"";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
