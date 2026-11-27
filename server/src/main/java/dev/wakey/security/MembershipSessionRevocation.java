package dev.wakey.security;

import dev.wakey.spaces.SpaceMemberRemoved;
import dev.wakey.spaces.SpaceMemberRoleChanged;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Ends the sessions of a member whose role changed or who left a space inside the membership
 * transaction; a failure here rolls the membership change back.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class MembershipSessionRevocation {

    private final UserSessions sessions;

    @EventListener
    void revokeOnMemberRemoved(SpaceMemberRemoved event) {
        log.debug("Ending sessions of subject {} after removal from space {}", event.subject(), event.spaceId());
        sessions.invalidateAll(event.issuer(), event.subject());
    }

    @EventListener
    void revokeOnMemberRoleChanged(SpaceMemberRoleChanged event) {
        log.debug("Ending sessions of subject {} after the role change in space {}", event.subject(), event.spaceId());
        sessions.invalidateAll(event.issuer(), event.subject());
    }
}
