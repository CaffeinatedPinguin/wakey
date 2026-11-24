package dev.wakey.security;

import org.springframework.security.core.Authentication;

/** The authenticated caller as the HTTP adapters see it: the OIDC subject and the application Admin flag. */
public record CurrentUser(String subject, boolean admin) {

    public static CurrentUser from(Authentication authentication) {
        return new CurrentUser(authentication.getName(), AdminAuthoritiesMapper.isAdmin(authentication));
    }
}
