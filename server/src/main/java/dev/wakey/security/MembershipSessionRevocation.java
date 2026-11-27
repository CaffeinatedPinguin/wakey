package dev.wakey.security;

import dev.wakey.spaces.SpaceMemberRemoved;
import dev.wakey.spaces.SpaceMemberRoleChanged;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Ends the sessions of a member whose role changed or who left a space inside the membership
 * transaction; a failure here rolls the membership change back.
 */
@Component
@RequiredArgsConstructor
class MembershipSessionRevocation {

    private final UserSessions sessions;

    @EventListener
    void revokeOnMemberRemoved(SpaceMemberRemoved event) {
        sessions.invalidateAll(event.issuer(), event.subject());
    }

    @EventListener
    void revokeOnMemberRoleChanged(SpaceMemberRoleChanged event) {
        sessions.invalidateAll(event.issuer(), event.subject());
    }
}
