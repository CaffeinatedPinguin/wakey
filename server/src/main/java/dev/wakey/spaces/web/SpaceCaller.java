package dev.wakey.spaces.web;

import dev.wakey.web.CurrentUser;

/**
 * A caller of an operation without a {@code {slug}} path variable: the user plus the one space an
 * API token may reach, or an empty slug for a session caller.
 */
public record SpaceCaller(CurrentUser user, String slug) {}
