package dev.wakey.api.checks;

import dev.wakey.security.CurrentUser;
import dev.wakey.spaces.SpacePrincipal;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceService;
import dev.wakey.spaces.SpaceView;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Resolves the caller and the space view, applying the read and write access rules. */
@Component
final class ChecksAccess {

    private final SpaceService spaces;

    ChecksAccess(SpaceService spaces) {
        this.spaces = spaces;
    }

    SpacePrincipal principal(Authentication authentication) {
        CurrentUser user = CurrentUser.from(authentication);
        return new SpacePrincipal(user.subject(), user.admin());
    }

    SpaceView read(Authentication authentication, String slug) {
        SpacePrincipal principal = principal(authentication);
        return ChecksErrors.mapped(() -> spaces.view(principal, slug));
    }

    SpaceView write(Authentication authentication, String slug) {
        SpaceView view = read(authentication, slug);
        boolean editable = view.admin() || view.role() == SpaceRole.EDITOR;
        if (!editable) {
            throw ChecksErrors.forbidden();
        }
        return view;
    }
}
