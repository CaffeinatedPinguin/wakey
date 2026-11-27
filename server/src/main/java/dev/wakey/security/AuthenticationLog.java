package dev.wakey.security;

import dev.wakey.audit.ActorKind;
import dev.wakey.audit.AuditActor;
import dev.wakey.audit.AuditEntry;
import dev.wakey.audit.AuditLog;
import dev.wakey.audit.AuditOutcome;
import dev.wakey.audit.AuditSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** INFO on successful login and logout, WARN on a failed login; never logs tokens or secrets. */
@Slf4j
@Component
@RequiredArgsConstructor
final class AuthenticationLog {

    private static final String SESSION_OBJECT_TYPE = "session";

    private static final String ANONYMOUS_OBJECT_ID = "anonymous";

    private final AuditLog auditLog;

    @EventListener
    void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        String subject = authentication.getName();
        boolean admin = AdminAuthoritiesMapper.isAdmin(authentication);
        log.info("login succeeded for subject {} (admin: {})", subject, admin);
        record("login", subject, AuditOutcome.SUCCESS, AuditActor.of(subject, false));
    }

    @EventListener
    void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String reason = event.getException().getClass().getSimpleName();
        log.warn("login failed: {}", reason);
        record("login", ANONYMOUS_OBJECT_ID, AuditOutcome.DENIED, new AuditActor(null, ActorKind.SYSTEM));
    }

    @EventListener
    void onLogoutSuccess(LogoutSuccessEvent event) {
        String subject = event.getAuthentication().getName();
        log.info("logout succeeded for {}", subject);
        record("logout", subject, AuditOutcome.SUCCESS, AuditActor.of(subject, false));
    }

    private void record(String action, String objectId, AuditOutcome outcome, AuditActor actor) {
        AuditEntry entry =
                new AuditEntry(action, SESSION_OBJECT_TYPE, objectId, outcome, AuditSource.UI, actor, null, "{}");
        auditLog.recordSecurityEvent(entry);
    }
}
