package dev.wakey.spaces.internal;

import dev.wakey.spaces.UserSessions;
import java.util.Set;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

/** Adapts the spaces port to Spring Session JDBC, which indexes sessions by principal name. */
final class SpringSessionUserSessions implements UserSessions {

    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    SpringSessionUserSessions(FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.sessions = sessions;
    }

    @Override
    public void invalidateAll(String subject) {
        Set<String> sessionIds = sessions.findByPrincipalName(subject).keySet();
        for (String sessionId : sessionIds) {
            sessions.deleteById(sessionId);
        }
    }
}
