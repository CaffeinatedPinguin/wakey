package dev.wakey.spaces;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

/** Removes every session of one user through the Spring Session JDBC principal index. */
@Component
@RequiredArgsConstructor
public class UserSessions {

    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    public void invalidateAll(String subject) {
        Set<String> sessionIds = sessions.findByPrincipalName(subject).keySet();
        for (String sessionId : sessionIds) {
            sessions.deleteById(sessionId);
        }
    }
}
