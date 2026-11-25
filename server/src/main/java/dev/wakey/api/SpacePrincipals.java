package dev.wakey.api;

import dev.wakey.security.CurrentUser;
import dev.wakey.spaces.SpacePrincipal;
import dev.wakey.web.ApiProblemException;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

/**
 * The one factory that turns an HTTP authentication into the spaces module's actor and applies the
 * API token rules: a token reaches only its own space and never manages tokens.
 */
public final class SpacePrincipals {

    private static final String NOT_FOUND = "not_found";

    private static final String INSUFFICIENT_SCOPE = "insufficient_scope";

    private SpacePrincipals() {}

    /** The actor without a space check, for callers that list spaces. */
    public static SpacePrincipal of(Authentication authentication) {
        CurrentUser user = CurrentUser.from(authentication);
        return new SpacePrincipal(user.subject(), user.admin());
    }

    /** The actor for one space, refusing a token that belongs to another space with a 404. */
    public static SpacePrincipal forSpace(Authentication authentication, String slug) {
        CurrentUser user = CurrentUser.from(authentication);
        requireTokenSpace(user, slug);
        return new SpacePrincipal(user.subject(), user.admin());
    }

    /** The one space of a token caller, or empty for a session caller. */
    public static Optional<String> tokenSpaceSlug(Authentication authentication) {
        CurrentUser user = CurrentUser.from(authentication);
        return Optional.ofNullable(user.tokenSpaceSlug());
    }

    /** Rejects a token caller; token management accepts a session only. */
    public static void requireUserSession(Authentication authentication) {
        CurrentUser user = CurrentUser.from(authentication);
        if (user.tokenSpaceSlug() != null) {
            throw new ApiProblemException(HttpStatus.FORBIDDEN, INSUFFICIENT_SCOPE);
        }
    }

    private static void requireTokenSpace(CurrentUser user, String slug) {
        @Nullable String tokenSpaceSlug = user.tokenSpaceSlug();
        if (tokenSpaceSlug != null && !tokenSpaceSlug.equals(slug)) {
            throw new ApiProblemException(HttpStatus.NOT_FOUND, NOT_FOUND);
        }
    }
}
