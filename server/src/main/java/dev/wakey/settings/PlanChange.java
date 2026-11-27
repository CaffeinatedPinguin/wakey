package dev.wakey.settings;

import dev.wakey.audit.AuditActor;
import java.util.UUID;

/** One finished plan change of a space, ready to be audited. */
public record PlanChange(UUID spaceId, String slug, String oldPlan, String newPlan, AuditActor actor) {}
