package dev.wakey.settings;

import java.util.UUID;

/** One finished plan change of a space, ready to be audited. */
public record PlanChange(UUID spaceId, String slug, String oldPlan, String newPlan, SettingActor actor) {}
