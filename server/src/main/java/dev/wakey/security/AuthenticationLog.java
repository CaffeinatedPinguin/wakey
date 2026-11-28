package dev.wakey.security;

import dev.wakey.audit.ActorKind;
import dev.wakey.audit.AuditActor;
import dev.wakey.audit.AuditEntry;
import dev.wakey.audit.AuditLog;
import dev.wakey.audit.AuditOutcome;
import dev.wakey.audit.AuditSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** INFO on successful login and logout, WARN on a failed login; never logs tokens or secrets. */
@Slf4j
@Component
@RequiredArgsConstructor
final class AuthenticationLog {

    private static final String SESSION_OBJECT_TYPE = "session";

    private static final String ANONYMOUS_OBJECT_ID = "anonymous";

    private static final String[] USER_NAME_CLAIMS = {"preferred_username", "email", "name"};

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final AuditLog auditLog;

    @EventListener
    void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        String subject = authentication.getName();
        String username = userName(authentication);
        boolean admin = AdminAuthoritiesMapper.isAdmin(authentication);
        log.info("login succeeded for subject {} (username: {}, admin: {})", subject, username, admin);
        String details = JSON.writeValueAsString(new LoginDetails(username, issuer(authentication)));
        recordSession("login", subject, AuditActor.of(subject, false), details);
    }

    @EventListener
    void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        Authentication authentication = event.getAuthentication();
        String reason = reason(event.getException());
        String address = address(authentication);
        String username = userName(authentication);
        log.warn("login failed: reason={} address={} username={}", reason, address, username);
        String details = JSON.writeValueAsString(new LoginFailureDetails(reason, address, username));
        recordFailure(details);
    }

    @EventListener
    void onLogoutSuccess(LogoutSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        String subject = authentication.getName();
        String username = userName(authentication);
        log.info("logout succeeded for subject {} (username: {})", subject, username);
        String details = JSON.writeValueAsString(new LoginDetails(username, issuer(authentication)));
        recordSession("logout", subject, AuditActor.of(subject, false), details);
    }

    private void recordSession(String action, String subject, AuditActor actor, String details) {
        AuditEntry entry = new AuditEntry(
                action, SESSION_OBJECT_TYPE, subject, AuditOutcome.SUCCESS, AuditSource.UI, actor, null, details);
        auditLog.recordSecurityEvent(entry);
    }

    private void recordFailure(String details) {
        AuditEntry entry = new AuditEntry(
                "login",
                SESSION_OBJECT_TYPE,
                ANONYMOUS_OBJECT_ID,
                AuditOutcome.DENIED,
                AuditSource.UI,
                new AuditActor(null, ActorKind.SYSTEM),
                null,
                details);
        auditLog.recordSecurityEvent(entry);
    }

    private static String reason(RuntimeException exception) {
        if (exception instanceof OAuth2AuthenticationException oauth2) {
            return oauth2.getError().getErrorCode();
        }
        return exception.getClass().getSimpleName();
    }

    private static @Nullable String address(Authentication authentication) {
        if (authentication.getDetails() instanceof WebAuthenticationDetails details) {
            return details.getRemoteAddress();
        }
        return null;
    }

    private static @Nullable String userName(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            return claimUserName(oidcUser);
        }
        return null;
    }

    private static @Nullable String claimUserName(OidcUser oidcUser) {
        for (String claim : USER_NAME_CLAIMS) {
            Object value = oidcUser.getClaim(claim);
            if (value instanceof String name && !name.isBlank()) {
                return name;
            }
        }
        return null;
    }

    private static @Nullable String issuer(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            Object issuer = oidcUser.getClaim("iss");
            if (issuer instanceof String text) {
                return text;
            }
        }
        return null;
    }

    private record LoginDetails(
            @Nullable String username, @Nullable String issuer) {}

    private record LoginFailureDetails(
            String reason,
            @Nullable String address,
            @Nullable String username) {}
}
