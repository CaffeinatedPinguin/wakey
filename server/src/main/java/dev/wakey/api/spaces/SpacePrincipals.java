package dev.wakey.api.spaces;

import dev.wakey.security.CurrentUser;
import dev.wakey.spaces.SpacePrincipal;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Builds the spaces principal from the authenticated caller. */
final class SpacePrincipals {

    private SpacePrincipals() {}

    static SpacePrincipal current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return of(Objects.requireNonNull(authentication));
    }

    static SpacePrincipal of(Authentication authentication) {
        CurrentUser user = CurrentUser.from(authentication);
        return new SpacePrincipal(user.subject(), user.admin());
    }
}
