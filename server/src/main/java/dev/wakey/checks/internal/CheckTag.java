package dev.wakey.checks.internal;

import java.util.UUID;

/**
 * One tag of a check, owned by the check aggregate and written with it. The space is copied onto
 * the row so the tag filter never has to look outside the space.
 */
public record CheckTag(UUID spaceId, String tag) {}
