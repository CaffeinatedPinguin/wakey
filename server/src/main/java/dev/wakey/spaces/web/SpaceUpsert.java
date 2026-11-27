package dev.wakey.spaces.web;

import dev.wakey.web.CurrentUser;

/**
 * The caller of the space upsert: the write rules of an existing space are applied before the body
 * is validated, while an absent slug stays available to an admin who creates the space.
 */
public record SpaceUpsert(CurrentUser user) {}
