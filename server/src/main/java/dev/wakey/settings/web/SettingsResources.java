package dev.wakey.settings.web;

import dev.wakey.audit.AuditActor;
import dev.wakey.settings.EffectiveSetting;
import dev.wakey.settings.PlanAssignment;
import dev.wakey.settings.SettingCatalog;
import dev.wakey.settings.SettingDefinition;
import dev.wakey.settings.SettingValue;
import dev.wakey.settings.SpacePlan;
import dev.wakey.settings.SpaceSettings;
import dev.wakey.spaces.SpaceView;
import dev.wakey.web.CurrentUser;
import dev.wakey.web.CursorPage;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** The write use cases of the settings HTTP adapter, including the audited plan change. */
@Component
@RequiredArgsConstructor
class SettingsResources {

    private final SpaceSettings settings;

    private final PlanAssignment assignments;

    private final SettingCatalog catalog;

    CursorPage<SpaceSettingResponse> list(SpaceView view) {
        SpacePlan plan = settings.planOf(view.id());
        var effective = settings.effectiveAll(view.id());
        List<SpaceSettingResponse> response = new ArrayList<>();
        for (var entry : effective.entrySet()) {
            String key = entry.getKey();
            EffectiveSetting setting = entry.getValue();
            SettingDefinition definition = catalog.require(key);
            response.add(SpaceSettingResponse.from(definition, setting, plan));
        }
        return new CursorPage<>(response, null);
    }

    void set(CurrentUser actor, SpaceView view, SettingValue value, boolean token) {
        AuditActor auditActor = AuditActor.of(actor.subject(), token);
        settings.set(view.id(), value, auditActor);
    }

    void reset(CurrentUser actor, SpaceView view, String key, boolean token) {
        AuditActor auditActor = AuditActor.of(actor.subject(), token);
        settings.reset(view.id(), key, auditActor);
    }

    void resetAll(CurrentUser actor, SpaceView view, boolean token) {
        AuditActor auditActor = AuditActor.of(actor.subject(), token);
        settings.resetAll(view.id(), auditActor);
    }

    void assign(CurrentUser actor, String slug, String requestedPlan, boolean token) {
        assignments.assign(actor, slug, requestedPlan, token);
    }
}
