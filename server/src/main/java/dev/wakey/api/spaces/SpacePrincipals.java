package dev.wakey.api.spaces;

import dev.wakey.security.CurrentUser;
import dev.wakey.spaces.SpacePrincipal;
import org.springframework.security.core.Authentication;

/** Builds the spaces principal from the authenticated caller. */
final class SpacePrincipals {

    private SpacePrincipals() {}

    static SpacePrincipal of(Authentication authentication) {
        CurrentUser user = CurrentUser.from(authentication);
        return new SpacePrincipal(user.subject(), user.admin());
    }
}
