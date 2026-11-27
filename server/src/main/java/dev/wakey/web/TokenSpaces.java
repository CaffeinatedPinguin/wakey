package dev.wakey.web;

import java.util.Optional;
import org.springframework.security.core.Authentication;

/** Reads the one space an API token may reach and enforces it in the HTTP adapters. */
public final class TokenSpaces {

    private TokenSpaces() {}

    public static Optional<String> of(Authentication authentication) {
        Optional<String> slug = Optional.empty();
        if (authentication instanceof TokenScopedAuthentication token) {
            slug = Optional.of(token.tokenSpaceSlug());
        }
        return slug;
    }

    public static void requireSpace(Authentication authentication, String slug) {
        Optional<String> tokenSpace = of(authentication);
        if (tokenSpace.isPresent() && !tokenSpace.get().equals(slug)) {
            throw new ApiException(CommonError.NOT_FOUND);
        }
    }
}
