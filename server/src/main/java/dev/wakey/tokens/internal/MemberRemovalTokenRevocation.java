package dev.wakey.tokens.internal;

import dev.wakey.spaces.SpaceMemberRemoved;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Revokes the removed member's tokens in that space inside the membership-removal transaction; a
 * failure here rolls the removal back.
 */
@Component
@RequiredArgsConstructor
class MemberRemovalTokenRevocation {

    private final TokenStore tokens;

    private final Clock clock;

    @EventListener
    void revokeOnMemberRemoved(SpaceMemberRemoved event) {
        Instant now = Instant.now(clock);
        tokens.revokeAll(event.spaceId(), event.issuer(), event.subject(), now);
    }
}
