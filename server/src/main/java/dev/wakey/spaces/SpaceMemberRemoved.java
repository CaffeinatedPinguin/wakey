package dev.wakey.spaces;

import java.util.UUID;

/**
 * Published when a member is removed from a space. The tokens module listens in the same
 * transaction and revokes that owner's tokens in the space, so a later re-invitation cannot revive
 * them.
 */
public record SpaceMemberRemoved(UUID spaceId, String issuer, String subject) {}
