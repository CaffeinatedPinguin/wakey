package dev.wakey.security;

import dev.wakey.web.ApiException;
import dev.wakey.web.CommonError;
import dev.wakey.web.CurrentUser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

/**
 * Lists and ends the browser sessions of one member. The Spring Session JDBC principal index stores
 * the subject only, so it finds the candidates and the stored authentication decides the issuer. The
 * session id stays internal: callers get a truncated SHA-256 identifier instead.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSessions {

    private static final String DIGEST_ALGORITHM = "SHA-256";

    private static final int OPAQUE_ID_LENGTH = 22;

    private static final Comparator<SessionView> NEWEST_FIRST =
            Comparator.comparing(SessionView::lastUsedAt).reversed().thenComparing(SessionView::id);

    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    public void invalidateAll(String issuer, String subject) {
        log.debug("Invalidating sessions of issuer {} and subject {}", issuer, subject);
        List<Session> ended = sessionsOf(issuer, subject);
        for (Session session : ended) {
            sessions.deleteById(session.getId());
        }
        log.info("Ended {} sessions of issuer {} and subject {}", ended.size(), issuer, subject);
    }

    public List<SessionView> list(CurrentUser user, @Nullable String currentSessionId) {
        List<SessionView> views = new ArrayList<>();
        for (Session session : sessionsOf(user.issuer(), user.subject())) {
            views.add(SessionView.of(session, currentSessionId));
        }
        views.sort(NEWEST_FIRST);
        log.debug("Listed {} sessions of subject {}", views.size(), user.subject());
        return List.copyOf(views);
    }

    public void revoke(CurrentUser user, String opaqueId) {
        Session target = find(user, opaqueId).orElseThrow(() -> new ApiException(CommonError.NOT_FOUND));
        sessions.deleteById(target.getId());
        log.info("Ended 1 session of issuer {} and subject {}", user.issuer(), user.subject());
    }

    public void revokeOthers(CurrentUser user, @Nullable String currentSessionId) {
        int ended = 0;
        for (Session session : sessionsOf(user.issuer(), user.subject())) {
            if (!Objects.equals(session.getId(), currentSessionId)) {
                sessions.deleteById(session.getId());
                ended++;
            }
        }
        log.info("Ended {} other sessions of issuer {} and subject {}", ended, user.issuer(), user.subject());
    }

    private Optional<Session> find(CurrentUser user, String opaqueId) {
        return sessionsOf(user.issuer(), user.subject()).stream()
                .filter(session -> opaqueId(session.getId()).equals(opaqueId))
                .findFirst();
    }

    private List<Session> sessionsOf(String issuer, String subject) {
        Map<String, ? extends Session> candidates = sessions.findByPrincipalName(subject);
        List<Session> matching = new ArrayList<>();
        for (Session session : candidates.values()) {
            if (belongsTo(session, issuer, subject)) {
                matching.add(session);
            }
        }
        return matching;
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

    private static String opaqueId(String sessionId) {
        try {
            MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
            byte[] hash = digest.digest(sessionId.getBytes(StandardCharsets.UTF_8));
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            return encoded.substring(0, OPAQUE_ID_LENGTH);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    /** One session as the caller may see it; {@code id} is an opaque hash, never the session id. */
    public record SessionView(String id, Instant createdAt, Instant lastUsedAt, Instant expiresAt, boolean current) {

        static SessionView of(Session session, @Nullable String currentSessionId) {
            String sessionId = session.getId();
            Instant lastUsedAt = session.getLastAccessedTime();
            Duration maxInactiveInterval = session.getMaxInactiveInterval();
            String opaqueId = opaqueId(sessionId);
            Instant expiresAt = lastUsedAt.plus(maxInactiveInterval);
            boolean current = Objects.equals(sessionId, currentSessionId);
            return new SessionView(opaqueId, session.getCreationTime(), lastUsedAt, expiresAt, current);
        }
    }
}
