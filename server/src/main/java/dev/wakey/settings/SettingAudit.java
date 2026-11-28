package dev.wakey.settings;

import dev.wakey.audit.AuditActor;
import dev.wakey.audit.AuditChange;
import dev.wakey.audit.AuditLog;
import dev.wakey.spaces.SpaceView;
import java.util.LinkedHashMap;
import java.util.Map;
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
        Map<String, Object> details = changeDetails(change.oldValue(), change.newValue());
        AuditActor actor = change.actor();
        AuditChange entry =
                new AuditChange(change.action(), SETTING_OBJECT_TYPE, change.key(), change.spaceId(), actor, details);
        auditLog.record(entry);
    }

    void planChanged(SpaceView space, String oldPlan, String newPlan, AuditActor actor) {
        Map<String, Object> details = changeDetails(oldPlan, newPlan);
        AuditChange entry = new AuditChange(PLAN_ACTION, SPACE_OBJECT_TYPE, space.slug(), space.id(), actor, details);
        auditLog.record(entry);
    }

    private static Map<String, Object> changeDetails(@Nullable String oldValue, @Nullable String newValue) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("old", oldValue);
        details.put("new", newValue);
        return details;
    }
}
