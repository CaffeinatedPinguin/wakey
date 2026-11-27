package dev.wakey.settings;

import dev.wakey.settings.internal.PlanCatalog;
import dev.wakey.spaces.SpaceService;
import dev.wakey.spaces.SpaceView;
import dev.wakey.web.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assigns a plan to a space. The admin rule belongs to the space writer; this use case resolves the
 * requested plan and records the change.
 */
@Service
@RequiredArgsConstructor
public class PlanAssignment {

    private final SpaceService spaces;

    private final PlanCatalog plans;

    private final SettingAudit audit;

    /**
     * Assigns a configured plan idempotently and records the change when the plan differs. The
     * writer checks the admin rule before the requested name is resolved, so a non-admin cannot
     * probe plan names; an unknown plan rolls the transaction back.
     */
    @Transactional
    public void assign(CurrentUser actor, String slug, String planName, boolean token) {
        SpaceView view = spaces.view(actor, slug);
        String oldPlan = plans.nameOrDefault(view.planName());
        spaces.assignPlan(actor, slug, planName);
        String resolvedPlan = plans.requireExisting(planName);
        if (!oldPlan.equals(resolvedPlan)) {
            SettingActor settingActor = new SettingActor(actor.subject(), token);
            PlanChange change = new PlanChange(view.id(), slug, oldPlan, resolvedPlan, settingActor);
            audit.planChanged(change);
        }
    }
}
