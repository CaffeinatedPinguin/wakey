package dev.wakey.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import dev.wakey.spaces.Space;
import dev.wakey.spaces.SpaceMember;
import dev.wakey.spaces.SpaceMembers;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceService;
import dev.wakey.testsupport.SecurityIntegrationTest;
import dev.wakey.tokens.ApiTokens;
import dev.wakey.tokens.CreatedToken;
import dev.wakey.tokens.TokenError;
import dev.wakey.tokens.TokenOptions;
import dev.wakey.tokens.TokenOwner;
import dev.wakey.tokens.TokenScope;
import dev.wakey.web.CommonError;
import dev.wakey.web.CurrentUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class MySessionsIntegrationTest extends SecurityIntegrationTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private static final String ISSUER = "https://issuer.example.com";

    private static final String SESSIONS = "/api/v1/me/sessions";

    private static final String SESSION_COOKIE_NAME = "SESSION";

    private static final String SESSION_COOKIE_PREFIX = SESSION_COOKIE_NAME + "=";

    private static final String ERROR_MEDIA_TYPE = "application/problem+json";

    private static final CurrentUser ADMIN = new CurrentUser(ISSUER, "fixture-admin", true);

    @Test
    void aUserListsOnlyTheirOwnSessions() throws Exception {
        // given
        String alice = uniqueSubject("alice");
        String bob = uniqueSubject("bob");
        Cookie aliceCookie = login(alice);
        login(alice);
        login(bob);

        // when
        MvcResult result = listSessions(aliceCookie);

        // then
        JsonNode items = jsonOf(result);
        int status = statusOf(result);
        assertThat(status).isEqualTo(200);
        assertThat(items.size()).isEqualTo(2);
    }

    @Test
    void theCurrentSessionIsMarked() throws Exception {
        // given
        String subject = uniqueSubject("current");
        Cookie currentCookie = login(subject);
        login(subject);

        // when
        MvcResult result = listSessions(currentCookie);

        // then
        JsonNode items = jsonOf(result);
        long marked = countCurrent(items);
        assertThat(statusOf(result)).isEqualTo(200);
        assertThat(items.size()).isEqualTo(2);
        assertThat(marked).isEqualTo(1);
    }

    @Test
    void revokingASessionEndsIt() throws Exception {
        // given
        String subject = uniqueSubject("revoke");
        Cookie currentCookie = login(subject);
        login(subject);
        String otherId = otherIdOf(currentCookie);

        // when
        MvcResult revoked = revokeSession(currentCookie, otherId);
        MvcResult listed = listSessions(currentCookie);

        // then
        JsonNode items = jsonOf(listed);
        assertThat(statusOf(revoked)).isEqualTo(204);
        assertThat(statusOf(listed)).isEqualTo(200);
        assertThat(items.size()).isEqualTo(1);
        assertThat(countCurrent(items)).isEqualTo(1);
        assertThat(sessionCount(subject)).isEqualTo(1);
    }

    @Test
    void revokingAnotherUsersSessionIsNotFound() throws Exception {
        // given
        String alice = uniqueSubject("other-alice");
        String bob = uniqueSubject("other-bob");
        Cookie aliceCookie = login(alice);
        Cookie bobCookie = login(bob);
        String bobId = firstIdOf(bobCookie);

        // when
        MvcResult result = revokeSession(aliceCookie, bobId);

        // then
        JsonNode body = jsonOf(result);
        String code = body.path("code").asString();
        assertThat(statusOf(result)).isEqualTo(404);
        assertThat(contentTypeOf(result)).isEqualTo(ERROR_MEDIA_TYPE);
        assertThat(code).isEqualTo(CommonError.NOT_FOUND.getCode());
        assertThat(sessionCount(bob)).isEqualTo(1);
    }

    @Test
    void logoutEverywhereKeepsOnlyTheCurrentSession() throws Exception {
        // given
        String subject = uniqueSubject("everywhere");
        String stranger = uniqueSubject("stranger");
        Cookie currentCookie = login(subject);
        login(subject);
        login(subject);
        login(stranger);

        // when
        MvcResult result = revokeAllSessions(currentCookie);
        MvcResult listed = listSessions(currentCookie);

        // then
        JsonNode items = jsonOf(listed);
        assertThat(statusOf(result)).isEqualTo(204);
        assertThat(statusOf(listed)).isEqualTo(200);
        assertThat(items.size()).isEqualTo(1);
        assertThat(countCurrent(items)).isEqualTo(1);
        assertThat(sessionCount(subject)).isEqualTo(1);
        assertThat(sessionCount(stranger)).isEqualTo(1);
    }

    @Test
    void anApiTokenCannotManageSessions() throws Exception {
        // given
        String subject = uniqueSubject("token");
        login(subject);
        CreatedToken token = createToken(subject);

        // when
        MvcResult listed = listWithToken(token);
        MvcResult revokedOne = revokeWithToken(token);
        MvcResult revokedAll = revokeAllWithToken(token);

        // then
        assertThat(statusOf(listed)).isEqualTo(403);
        assertThat(statusOf(revokedOne)).isEqualTo(403);
        assertThat(statusOf(revokedAll)).isEqualTo(403);
        String insufficientScope = TokenError.INSUFFICIENT_SCOPE.getCode();
        assertThat(codeOf(listed)).isEqualTo(insufficientScope);
        assertThat(codeOf(revokedOne)).isEqualTo(insufficientScope);
        assertThat(codeOf(revokedAll)).isEqualTo(insufficientScope);
        assertThat(sessionCount(subject)).isEqualTo(1);
    }

    @Test
    void theSessionListNeverExposesTheSessionId() throws Exception {
        // given
        String subject = uniqueSubject("opaque");
        Cookie currentCookie = login(subject);
        login(subject);

        // when
        MvcResult result = listSessions(currentCookie);

        // then
        JsonNode items = jsonOf(result);
        String body = result.getResponse().getContentAsString();
        List<String> ids = idsOf(items);
        List<String> sessionIds = sessionIdsOf(subject);
        assertThat(statusOf(result)).isEqualTo(200);
        assertThat(ids).hasSize(2);
        assertThat(ids).doesNotContainAnyElementsOf(sessionIds);
        for (String sessionId : sessionIds) {
            assertThat(body).doesNotContain(sessionId);
        }
    }

    private String otherIdOf(Cookie cookie) throws Exception {
        MvcResult result = listSessions(cookie);
        JsonNode items = jsonOf(result);
        return otherItem(items).path("id").asString();
    }

    private String firstIdOf(Cookie cookie) throws Exception {
        MvcResult result = listSessions(cookie);
        JsonNode items = jsonOf(result);
        assertThat(items.size()).isEqualTo(1);
        return itemOf(items, 0).path("id").asString();
    }

    private Cookie login(String subject) throws Exception {
        var login = oidcLogin().idToken(token -> token.subject(subject).issuer(ISSUER));
        MockHttpServletRequestBuilder request =
                get(SessionLoginEndpoint.TEST_LOGIN).with(login);
        MvcResult result = mockMvc().perform(request).andReturn();
        assertThat(statusOf(result)).isEqualTo(200);
        return sessionCookie(result);
    }

    private static Cookie sessionCookie(MvcResult result) {
        for (String header : result.getResponse().getHeaders(HttpHeaders.SET_COOKIE)) {
            if (header.startsWith(SESSION_COOKIE_PREFIX)) {
                String pair = header.split(";", 2)[0];
                String value = pair.substring(SESSION_COOKIE_PREFIX.length());
                return new Cookie(SESSION_COOKIE_NAME, unquote(value));
            }
        }
        throw new AssertionError("No session cookie");
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private MvcResult listSessions(Cookie cookie) throws Exception {
        MockHttpServletRequestBuilder request = withCookie(get(SESSIONS), cookie);
        return mockMvc().perform(request).andReturn();
    }

    private MvcResult revokeSession(Cookie cookie, String id) throws Exception {
        MockHttpServletRequestBuilder deletion = delete(SESSIONS + "/" + id).with(csrf());
        MockHttpServletRequestBuilder request = withCookie(deletion, cookie);
        return mockMvc().perform(request).andReturn();
    }

    private MvcResult revokeAllSessions(Cookie cookie) throws Exception {
        MockHttpServletRequestBuilder deletion = delete(SESSIONS).with(csrf());
        MockHttpServletRequestBuilder request = withCookie(deletion, cookie);
        return mockMvc().perform(request).andReturn();
    }

    private MvcResult listWithToken(CreatedToken token) throws Exception {
        MockHttpServletRequestBuilder request = withToken(get(SESSIONS), token);
        return mockMvc().perform(request).andReturn();
    }

    private MvcResult revokeWithToken(CreatedToken token) throws Exception {
        MockHttpServletRequestBuilder request = withToken(delete(SESSIONS + "/anything"), token);
        return mockMvc().perform(request).andReturn();
    }

    private MvcResult revokeAllWithToken(CreatedToken token) throws Exception {
        MockHttpServletRequestBuilder request = withToken(delete(SESSIONS), token);
        return mockMvc().perform(request).andReturn();
    }

    private static JsonNode otherItem(JsonNode items) {
        for (JsonNode item : items) {
            if (!item.path("current").asBoolean()) {
                return item;
            }
        }
        throw new AssertionError("No other session");
    }

    private static JsonNode itemOf(JsonNode items, int index) {
        return Objects.requireNonNull(items.get(index));
    }

    private static long countCurrent(JsonNode items) {
        long current = 0;
        for (JsonNode item : items) {
            if (item.path("current").asBoolean()) {
                current++;
            }
        }
        return current;
    }

    private static List<String> idsOf(JsonNode items) {
        List<String> ids = new ArrayList<>();
        for (JsonNode item : items) {
            String id = item.path("id").asString();
            ids.add(id);
        }
        return ids;
    }

    private CreatedToken createToken(String subject) {
        SpaceMembers members = bean(SpaceMembers.class);
        String slug = "token-" + UUID.randomUUID().toString().substring(0, 8);
        Space space =
                bean(SpaceService.class).put(ADMIN, slug, "Token space", null).space();
        members.addMember(ADMIN, space.slug(), new SpaceMember(ISSUER, subject, SpaceRole.EDITOR));
        TokenOwner owner = new TokenOwner(ISSUER, subject);
        TokenOptions options = new TokenOptions(TokenScope.WRITE, null);
        return bean(ApiTokens.class).create(owner, space.slug(), "sessions", options);
    }

    private int sessionCount(String subject) {
        return sessionsOf(subject).size();
    }

    private List<String> sessionIdsOf(String subject) {
        List<String> ids = new ArrayList<>();
        for (Session session : sessionsOf(subject)) {
            ids.add(session.getId());
        }
        return ids;
    }

    private List<Session> sessionsOf(String subject) {
        JdbcIndexedSessionRepository repository = bean(JdbcIndexedSessionRepository.class);
        var sessions = repository.findByPrincipalName(subject);
        return List.copyOf(sessions.values());
    }

    private static MockHttpServletRequestBuilder withCookie(MockHttpServletRequestBuilder request, Cookie cookie) {
        return request.cookie(cookie);
    }

    private static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, CreatedToken token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.secret());
    }

    private static String codeOf(MvcResult result) throws Exception {
        return jsonOf(result).path("code").asString();
    }

    private static String contentTypeOf(MvcResult result) {
        String contentType = result.getResponse().getContentType();
        return Objects.requireNonNull(contentType);
    }

    private static int statusOf(MvcResult result) {
        return result.getResponse().getStatus();
    }

    private static JsonNode jsonOf(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();
        return MAPPER.readTree(content);
    }

    private static String uniqueSubject(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    @TestConfiguration
    static class SessionLoginConfiguration {

        @Bean
        SessionLoginEndpoint sessionLoginEndpoint() {
            return new SessionLoginEndpoint();
        }
    }

    /** Creates a real Spring Session from the authenticated caller so a test can reuse its cookie. */
    @RestController
    static final class SessionLoginEndpoint {

        private static final String TEST_LOGIN = "/api/v1/test-login";

        @GetMapping(TEST_LOGIN)
        void login(HttpServletRequest request) {
            Authentication authentication = Objects.requireNonNull((Authentication) request.getUserPrincipal());
            SecurityContextImpl context = new SecurityContextImpl(authentication);
            request.getSession(true)
                    .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        }
    }
}
