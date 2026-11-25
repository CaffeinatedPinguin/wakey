package dev.wakey.security;

import org.springframework.security.core.Authentication;

/** The authenticated caller: the OIDC subject and whether the user is an application Admin. */
public record CurrentUser(String subject, boolean admin) {

    public static CurrentUser from(Authentication authentication) {
        boolean admin = AdminAuthoritiesMapper.isAdmin(authentication);
        return new CurrentUser(authentication.getName(), admin);
    }
}
