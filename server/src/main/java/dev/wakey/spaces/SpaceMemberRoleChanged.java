package dev.wakey.spaces;

import java.util.UUID;

/**
 * Published when a member's role really changes. The security module listens in the same transaction
 * and ends that user's sessions, so the smaller role applies to every new request.
 */
public record SpaceMemberRoleChanged(UUID spaceId, String issuer, String subject) {}
