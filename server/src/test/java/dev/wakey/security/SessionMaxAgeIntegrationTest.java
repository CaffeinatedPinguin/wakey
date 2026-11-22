package dev.wakey.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import dev.wakey.testsupport.SecurityIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@TestPropertySource(properties = "wakey.security.session-max-age=2s")
class SessionMaxAgeIntegrationTest extends SecurityIntegrationTest {

    @Test
    void sessionOlderThanMaxAgeIsRejected() throws Exception {
        // given
        JdbcIndexedSessionRepository sessions = bean(JdbcIndexedSessionRepository.class);
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                "alice", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        Session session = createAuthenticatedSession(sessions, authentication);
        long createdAt = Instant.now().minusSeconds(60).toEpochMilli();
        jdbc().sql("UPDATE SPRING_SESSION SET CREATION_TIME = :createdAt WHERE SESSION_ID = :id")
                .param("createdAt", createdAt)
                .param("id", session.getId())
                .update();
        Cookie cookie = new Cookie("SESSION", session.getId());
        MockHttpServletRequestBuilder request = get("/api/v1/me").cookie(cookie);

        // when
        MvcResult result = mockMvc().perform(request).andReturn();

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(401);
    }

    private static <S extends Session> S createAuthenticatedSession(
            SessionRepository<S> repository, Authentication authentication) {
        S session = repository.createSession();
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(authentication));
        repository.save(session);
        return session;
    }
}
