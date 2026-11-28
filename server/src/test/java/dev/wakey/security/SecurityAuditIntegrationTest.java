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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Security events reach the audit log: login, failed login, logout and rejected requests. */
@ExtendWith(OutputCaptureExtension.class)
class SecurityAuditIntegrationTest extends SecurityIntegrationTest {

    private static final String ISSUER = "https://issuer.example.com";

    private static final String SESSION = "session";

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static final String REQUEST = "request";

    @Autowired
    private ApplicationEventPublisher events;

    private CapturedOutput output;

    @BeforeEach
    void captureOutput(CapturedOutput output) {
        this.output = output;
    }

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
    void aSuccessfulLoginIsAuditedWithTheUserName() {
        // given
        Authentication authentication =
                oidcAuthentication(Map.of("sub", "success-subject", "iss", ISSUER, "preferred_username", "alice"));

        // when
        events.publishEvent(new AuthenticationSuccessEvent(authentication));
        List<AuditRecord> history = history(SESSION, "success-subject");

        // then
        assertThat(history).isNotEmpty();
        AuditRecord entry = history.getFirst();
        JsonNode details = details(entry);
        String subject = entry.actor().subject();
        assertThat(entry.action()).isEqualTo("login");
        assertThat(entry.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(subject).isEqualTo("success-subject");
        assertThat(text(details, "username")).isEqualTo("alice");
        assertThat(text(details, "issuer")).isEqualTo(ISSUER);
        assertThat(output.getOut()).contains("username: alice");
    }

    @Test
    void aUserNameFallsBackToTheEmail() {
        // given
        Authentication authentication = oidcAuthentication(
                Map.of("sub", "email-subject", "iss", ISSUER, "email", "bob@example.com", "name", "Bob"));

        // when
        events.publishEvent(new AuthenticationSuccessEvent(authentication));
        List<AuditRecord> history = history(SESSION, "email-subject");

        // then
        AuditRecord entry = history.getFirst();
        JsonNode details = details(entry);
        assertThat(text(details, "username")).isEqualTo("bob@example.com");
    }

    @Test
    void aFailedLoginIsAuditedWithItsReasonAndAddress() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.unauthenticated("unknown", "n/a");
        authentication.setDetails(new WebAuthenticationDetails(request));
        OAuth2AuthenticationException failure = new OAuth2AuthenticationException(new OAuth2Error("access_denied"));

        // when
        events.publishEvent(new AuthenticationFailureBadCredentialsEvent(authentication, failure));
        List<AuditRecord> history = history(SESSION, "anonymous");

        // then
        AuditRecord entry = history.getFirst();
        JsonNode details = details(entry);
        assertThat(entry.action()).isEqualTo("login");
        assertThat(entry.outcome()).isEqualTo(AuditOutcome.DENIED);
        assertThat(text(details, "reason")).isEqualTo("access_denied");
        assertThat(text(details, "address")).isEqualTo("192.0.2.10");
        assertThat(isNull(details, "username")).isTrue();
        assertThat(output.getOut()).contains("reason=access_denied").contains("address=192.0.2.10");
    }

    @Test
    void aFailedLoginWithAKnownIdentityIsAuditedWithTheUserName() {
        // given
        Authentication authentication =
                oidcAuthentication(Map.of("sub", "known-subject", "iss", ISSUER, "preferred_username", "carol"));
        OAuth2AuthenticationException failure = new OAuth2AuthenticationException(new OAuth2Error("access_denied"));

        // when
        events.publishEvent(new AuthenticationFailureBadCredentialsEvent(authentication, failure));
        List<AuditRecord> history = history(SESSION, "anonymous");

        // then
        AuditRecord entry = history.getFirst();
        JsonNode details = details(entry);
        assertThat(text(details, "username")).isEqualTo("carol");
    }

    @Test
    void aLogoutIsAuditedWithTheUserName() {
        // given
        Authentication authentication =
                oidcAuthentication(Map.of("sub", "logout-user", "iss", ISSUER, "preferred_username", "dave"));

        // when
        events.publishEvent(new LogoutSuccessEvent(authentication));
        List<AuditRecord> history = history(SESSION, "logout-user");

        // then
        AuditRecord entry = history.getFirst();
        JsonNode details = details(entry);
        assertThat(entry.action()).isEqualTo("logout");
        assertThat(text(details, "username")).isEqualTo("dave");
        assertThat(text(details, "issuer")).isEqualTo(ISSUER);
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

    private static Authentication oidcAuthentication(Map<String, Object> claims) {
        Instant issuedAt = Instant.EPOCH;
        Instant expiresAt = issuedAt.plusSeconds(600);
        OidcIdToken idToken = new OidcIdToken("id-token", issuedAt, expiresAt, claims);
        OidcUserAuthority authority = new OidcUserAuthority(idToken);
        OidcUser user = new DefaultOidcUser(List.of(authority), idToken);
        return UsernamePasswordAuthenticationToken.authenticated(user, "n/a", List.of(authority));
    }

    private static JsonNode details(AuditRecord entry) {
        return MAPPER.readTree(entry.details());
    }

    private static String text(JsonNode details, String field) {
        return details.path(field).asString();
    }

    private static boolean isNull(JsonNode details, String field) {
        return details.path(field).isNull();
    }

    private List<AuditRecord> history(String objectType, String objectId) {
        AuditPage page = new AuditPage(null, 20);
        return bean(AuditLog.class).history(null, objectType, objectId, page);
    }
}
