package dev.wakey.spaces.web;

import dev.wakey.spaces.SpaceView;
import dev.wakey.web.CurrentUser;

/**
 * The caller and the space read for a write; a viewer is refused before the body is validated.
 * {@code viaToken} tells whether the call was authenticated by an API token.
 */
public record SpaceWriter(CurrentUser user, SpaceView space, boolean viaToken) {}
