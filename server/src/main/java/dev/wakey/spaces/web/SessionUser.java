package dev.wakey.spaces.web;

import dev.wakey.web.CurrentUser;

/** A caller that must come from a session; API tokens never manage tokens. */
public record SessionUser(CurrentUser user) {}
