package dev.wakey.security;

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
final class AuthenticationLog {

    @EventListener
    void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        String subject = authentication.getName();
        boolean admin = AdminAuthoritiesMapper.isAdmin(authentication);
        log.info("login succeeded for subject {} (admin: {})", subject, admin);
    }

    @EventListener
    void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String reason = event.getException().getClass().getSimpleName();
        log.warn("login failed: {}", reason);
    }

    @EventListener
    void onLogoutSuccess(LogoutSuccessEvent event) {
        String subject = event.getAuthentication().getName();
        log.info("logout succeeded for {}", subject);
    }
}
