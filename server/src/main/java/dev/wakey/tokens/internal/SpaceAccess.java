package dev.wakey.tokens.internal;

import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceService;
import dev.wakey.spaces.SpaceView;
import dev.wakey.tokens.TokenScope;
import dev.wakey.web.ApiException;
import dev.wakey.web.CommonError;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Reads the owner's membership from the spaces module; an outsider sees the space as missing. */
@Component
@RequiredArgsConstructor
public final class SpaceAccess {

    private final SpaceService spaces;

    public SpaceView requireMember(String spaceSlug, String issuer, String subject) {
        return spaces.viewForMember(issuer, subject, spaceSlug);
    }

    /** Reads the owner's current role without treating a missing membership as an exception. */
    public Optional<Membership> findMembership(UUID spaceId, String issuer, String subject) {
        return spaces.viewForMemberById(spaceId, issuer, subject)
                .flatMap(view -> Optional.ofNullable(view.role()).map(role -> new Membership(view.slug(), role)));
    }

    public static void requireWriteTokenAllowed(SpaceView space, TokenScope scope) {
        if (scope == TokenScope.WRITE && space.role() == SpaceRole.VIEWER) {
            throw new ApiException(CommonError.FORBIDDEN);
        }
    }

    /** The membership values a token needs: the space slug and the owner's current role in it. */
    public record Membership(String slug, SpaceRole role) {

        public TokenScope effectiveScope(TokenScope requested) {
            return role == SpaceRole.VIEWER ? TokenScope.READ : requested;
        }
    }
}
