package dev.wakey.web;

import java.net.URL;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * The authenticated caller: the OIDC issuer and subject pair and whether the user is an application
 * Admin. An {@link OidcUser} supplies both values from the token; an API token supplies the issuer
 * and subject of its owner; every other authentication type (tests, sessions) uses
 * {@link #LOCAL_ISSUER} and {@link Authentication#getName()} as its subject.
 */
public record CurrentUser(String issuer, String subject, boolean admin) {

    public static final String ADMIN_ROLE = "ROLE_ADMIN";

    public static final String LOCAL_ISSUER = "urn:wakey:local";

    public static CurrentUser from(Authentication authentication) {
        boolean admin = isAdmin(authentication);
        if (authentication instanceof TokenScopedAuthentication token) {
            return new CurrentUser(token.ownerIssuer(), token.ownerSubject(), admin);
        }
        return fromPrincipal(authentication, admin);
    }

    private static CurrentUser fromPrincipal(Authentication authentication, boolean admin) {
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            URL issuerUrl = Objects.requireNonNull(oidcUser.getIssuer());
            String subject = Objects.requireNonNull(oidcUser.getSubject());
            return new CurrentUser(issuerUrl.toString(), subject, admin);
        }
        return new CurrentUser(LOCAL_ISSUER, authentication.getName(), admin);
    }

    /** Rejects a caller who is not an application Admin; the one place of this rule. */
    public void requireAdmin() {
        if (!admin) {
            throw new ApiException(CommonError.FORBIDDEN);
        }
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ADMIN_ROLE::equals);
    }
}
