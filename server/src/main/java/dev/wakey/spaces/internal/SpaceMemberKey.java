package dev.wakey.spaces.internal;

import java.util.UUID;

/** The composite key of a membership: {@code (space_id, subject)}. */
public record SpaceMemberKey(UUID spaceId, String subject) {}
