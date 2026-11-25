package dev.wakey.tokens.internal;

import dev.wakey.spaces.SpaceAccessDeniedException;
import dev.wakey.spaces.SpaceNotFoundException;
import dev.wakey.spaces.SpacePrincipal;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceService;
import dev.wakey.spaces.SpaceView;
import dev.wakey.tokens.TokenScope;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Reads the owner's membership from the spaces module; an outsider sees the space as missing. */
@Slf4j
@RequiredArgsConstructor
final class SpaceAccess {

    private final SpaceService spaces;

    SpaceView requireMember(String spaceSlug, String subject) {
        return spaces.view(new SpacePrincipal(subject, false), spaceSlug);
    }

    Optional<SpaceRole> findRole(String spaceSlug, String subject) {
        try {
            SpaceView view = requireMember(spaceSlug, subject);
            return Optional.ofNullable(view.role());
        } catch (SpaceNotFoundException exception) {
            log.debug("Token owner {} is not a member of space {}", subject, spaceSlug, exception);
            return Optional.empty();
        }
    }

    static void requireWriteTokenAllowed(SpaceView space, TokenScope scope) {
        if (scope == TokenScope.WRITE && space.role() == SpaceRole.VIEWER) {
            throw new SpaceAccessDeniedException("A viewer cannot create a write token in space " + space.slug());
        }
    }
}
