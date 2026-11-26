package dev.wakey.spaces;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

/**
 * Removes every session of one member through the Spring Session JDBC principal index. The index
 * stores the principal name only and cannot distinguish issuers, so the session principal name is
 * pinned to the OIDC subject ({@code spring.security.oauth2.client.provider.oidc.user-name-attribute}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSessions {

    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    public void invalidateAll(String issuer, String subject) {
        log.debug("Invalidating sessions of issuer {} and subject {}", issuer, subject);
        Set<String> sessionIds = sessions.findByPrincipalName(subject).keySet();
        for (String sessionId : sessionIds) {
            sessions.deleteById(sessionId);
        }
    }
}
