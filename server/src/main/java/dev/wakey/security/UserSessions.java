package dev.wakey.security;

import dev.wakey.web.CurrentUser;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

/**
 * Removes the sessions of one member. The Spring Session JDBC principal index stores the subject
 * only, so it finds the candidates and the stored authentication decides the issuer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSessions {

    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    public void invalidateAll(String issuer, String subject) {
        log.debug("Invalidating sessions of issuer {} and subject {}", issuer, subject);
        Map<String, ? extends Session> candidates = sessions.findByPrincipalName(subject);
        int ended = 0;
        for (Session session : candidates.values()) {
            if (belongsTo(session, issuer, subject)) {
                sessions.deleteById(session.getId());
                ended++;
            }
        }
        log.info("Ended {} sessions of issuer {} and subject {}", ended, issuer, subject);
    }

    private static boolean belongsTo(Session session, String issuer, String subject) {
        Object attribute = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        if (attribute instanceof SecurityContext context) {
            Authentication authentication = context.getAuthentication();
            return authentication != null && isSameUser(authentication, issuer, subject);
        }
        return false;
    }

    private static boolean isSameUser(Authentication authentication, String issuer, String subject) {
        CurrentUser user = CurrentUser.from(authentication);
        return user.issuer().equals(issuer) && user.subject().equals(subject);
    }
}
