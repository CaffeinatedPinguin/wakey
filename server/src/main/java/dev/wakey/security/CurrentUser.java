package dev.wakey.security;

import java.net.URL;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * The authenticated caller: the OIDC issuer and subject pair and whether the user is an application
 * Admin. An {@link OidcUser} supplies both values from the token; every other authentication type
 * (tests, future tokens) uses {@link #LOCAL_ISSUER} and {@link Authentication#getName()} as its
 * subject.
 */
public record CurrentUser(String issuer, String subject, boolean admin) {

    public static final String LOCAL_ISSUER = "urn:wakey:local";

    public static CurrentUser from(Authentication authentication) {
        boolean admin = AdminAuthoritiesMapper.isAdmin(authentication);
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            URL issuerUrl = Objects.requireNonNull(oidcUser.getIssuer());
            String subject = Objects.requireNonNull(oidcUser.getSubject());
            String issuer = issuerUrl.toString();
            return new CurrentUser(issuer, subject, admin);
        }
        return new CurrentUser(LOCAL_ISSUER, authentication.getName(), admin);
    }
}
