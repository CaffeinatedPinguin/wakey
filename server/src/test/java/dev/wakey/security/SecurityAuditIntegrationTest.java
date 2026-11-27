package dev.wakey.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import dev.wakey.audit.AuditLog;
import dev.wakey.audit.AuditOutcome;
import dev.wakey.audit.AuditPage;
import dev.wakey.audit.AuditRecord;
import dev.wakey.testsupport.SecurityIntegrationTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Security events reach the audit log: login, failed login, logout and rejected requests. */
class SecurityAuditIntegrationTest extends SecurityIntegrationTest {

    private static final String ISSUER = "https://issuer.example.com";

    private static final String SESSION = "session";

    private static final String REQUEST = "request";

    @Autowired
    private ApplicationEventPublisher events;

    @Test
    void aLoginIsAudited() {
        // given
        Authentication authentication = new UsernamePasswordAuthenticationToken("login-subject", "n/a");

        // when
        events.publishEvent(new AuthenticationSuccessEvent(authentication));
        List<AuditRecord> history = history(SESSION, "login-subject");

        // then
        assertThat(history).isNotEmpty();
        AuditRecord entry = history.getFirst();
        String subject = entry.actor().subject();
        assertThat(entry.action()).isEqualTo("login");
        assertThat(entry.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(subject).isEqualTo("login-subject");
        assertThat(entry.spaceId()).isNull();
    }

    @Test
    void aFailedLoginIsAudited() {
        // given
        Authentication authentication = new UsernamePasswordAuthenticationToken("failed-subject", "n/a");
        BadCredentialsException failure = new BadCredentialsException("n/a");

        // when
        events.publishEvent(new AuthenticationFailureBadCredentialsEvent(authentication, failure));
        List<AuditRecord> history = history(SESSION, "anonymous");

        // then
        assertThat(history).isNotEmpty();
        AuditRecord entry = history.getFirst();
        assertThat(entry.action()).isEqualTo("login");
        assertThat(entry.outcome()).isEqualTo(AuditOutcome.DENIED);
        assertThat(entry.spaceId()).isNull();
    }

    @Test
    void aLogoutIsAudited() throws Exception {
        // given
        RequestPostProcessor login =
                oidcLogin().idToken(token -> token.subject("logout-subject").issuer(ISSUER));
        MockHttpServletRequestBuilder request =
                post("/logout").secure(true).with(login).with(csrf());

        // when
        MvcResult result = mockMvc().perform(request).andReturn();
        List<AuditRecord> history = history(SESSION, "logout-subject");

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(200);
        assertThat(history).isNotEmpty();
        AuditRecord entry = history.getFirst();
        String subject = entry.actor().subject();
        assertThat(entry.action()).isEqualTo("logout");
        assertThat(entry.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(subject).isEqualTo("logout-subject");
    }

    @Test
    void aForbiddenRequestIsAudited() throws Exception {
        // given
        String path = "/api/v1/denied-" + UUID.randomUUID();
        RequestPostProcessor login =
                oidcLogin().idToken(token -> token.subject("forbidden-subject").issuer(ISSUER));
        MockHttpServletRequestBuilder request = post(path).with(login);

        // when
        MvcResult result = mockMvc().perform(request).andReturn();
        List<AuditRecord> history = history(REQUEST, "csrf-token-missing");

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(403);
        assertThat(history).isNotEmpty();
        AuditRecord entry = history.getFirst();
        String subject = entry.actor().subject();
        assertThat(subject).isEqualTo("forbidden-subject");
        assertThat(entry.outcome()).isEqualTo(AuditOutcome.DENIED);
    }

    @Test
    void anUnauthorizedRequestIsAudited() throws Exception {
        // when
        MvcResult result = mockMvc().perform(get("/agent/v1/checks")).andReturn();
        List<AuditRecord> history = history(REQUEST, "missing-authentication");

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(401);
        assertThat(history).isNotEmpty();
        AuditRecord entry = history.getFirst();
        assertThat(entry.action()).isEqualTo("access.denied");
        assertThat(entry.outcome()).isEqualTo(AuditOutcome.DENIED);
    }

    private List<AuditRecord> history(String objectType, String objectId) {
        AuditPage page = new AuditPage(null, 20);
        return bean(AuditLog.class).history(null, objectType, objectId, page);
    }
}
