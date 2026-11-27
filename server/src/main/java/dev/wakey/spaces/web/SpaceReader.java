package dev.wakey.spaces.web;

import dev.wakey.spaces.SpaceView;
import dev.wakey.web.CurrentUser;

/** The caller and the space read from the {@code {slug}} path variable. */
public record SpaceReader(CurrentUser user, SpaceView space) {}
