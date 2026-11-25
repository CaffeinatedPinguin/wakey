package dev.wakey.api.checks;

import dev.wakey.api.SpacePrincipals;
import dev.wakey.spaces.SpaceAccessDeniedException;
import dev.wakey.spaces.SpacePrincipal;
import dev.wakey.spaces.SpaceService;
import dev.wakey.spaces.SpaceView;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Resolves the caller and the space view, applying the read and write access rules. */
@Component
final class ChecksAccess {

    private static final String WRITE_DENIED = "Only an editor or an administrator can change checks";

    private final SpaceService spaces;

    ChecksAccess(SpaceService spaces) {
        this.spaces = spaces;
    }

    SpaceView read(Authentication authentication, String slug) {
        SpacePrincipal principal = SpacePrincipals.forSpace(authentication, slug);
        return spaces.view(principal, slug);
    }

    SpaceView write(Authentication authentication, String slug) {
        SpaceView view = read(authentication, slug);
        if (!view.canWrite()) {
            throw new SpaceAccessDeniedException(WRITE_DENIED);
        }
        return view;
    }
}
