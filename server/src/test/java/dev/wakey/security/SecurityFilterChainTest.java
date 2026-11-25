package dev.wakey.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.wakey.testsupport.SecurityIntegrationTest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class SecurityFilterChainTest extends SecurityIntegrationTest {

    private static final ResultMatcher OK = status().isOk();

    private static final ResultMatcher UNAUTHORIZED = status().isUnauthorized();

    private static final ResultMatcher CONTENT_SECURITY_POLICY =
            header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");

    private static final ResultMatcher REFERRER_POLICY = header().string("Referrer-Policy", "no-referrer");

    private static final ResultMatcher PERMISSIONS_POLICY =
            header().string("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

    @ParameterizedTest
    @ValueSource(strings = {"/agent/v1/checks", "/ping/token", "/public/v1/status"})
    void statelessEndpointsAreRejectedWithoutAuthentication(String path) throws Exception {
        // when
        MvcResult result = mockMvc().perform(get(path)).andReturn();

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(401);
    }

    @Test
    void statelessRequestsDoNotCreateSessions() throws Exception {
        // when
        MvcResult result = mockMvc()
                .perform(get("/agent/v1/checks"))
                .andExpect(UNAUTHORIZED)
                .andReturn();

        // then
        HttpSession session = result.getRequest().getSession(false);
        assertThat(session).isNull();
    }

    @Test
    void statelessSurfaceIgnoresTheSessionCookie() throws Exception {
        // given
        MockHttpSession session = authenticatedSession();
        Cookie sessionCookie = new Cookie("SESSION", session.getId());
        MockHttpServletRequestBuilder agentGet =
                get("/agent/v1/checks").session(session).cookie(sessionCookie);
        MockHttpServletRequestBuilder agentPost =
                post("/agent/v1/checks").session(session).cookie(sessionCookie);
        MockHttpServletRequestBuilder pingGet =
                get("/ping/token").session(session).cookie(sessionCookie);
        MockHttpServletRequestBuilder pingPost =
                post("/ping/token").session(session).cookie(sessionCookie);
        List<MockHttpServletRequestBuilder> requests = List.of(agentGet, agentPost, pingGet, pingPost);
        List<Integer> statuses = new ArrayList<>();

        // when
        for (MockHttpServletRequestBuilder request : requests) {
            MvcResult result = mockMvc().perform(request).andReturn();
            int status = result.getResponse().getStatus();
            statuses.add(status);
        }

        // then
        assertThat(statuses).containsOnly(401);
    }

    @Test
    void healthzIsPublic() throws Exception {
        mockMvc().perform(get("/healthz")).andExpect(OK);
    }

    @Test
    void apiRequestsRequireAuthentication() throws Exception {
        mockMvc().perform(get("/api/v1/me")).andExpect(UNAUTHORIZED);
    }

    @Test
    void meReturnsSubjectAndAdminFlag() throws Exception {
        // given
        var login = oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
        var request = get("/api/v1/me").with(login);

        // when
        MvcResult result = mockMvc().perform(request).andReturn();

        // then
        int status = result.getResponse().getStatus();
        String body = result.getResponse().getContentAsString();
        assertThat(status).isEqualTo(200);
        assertThat(body).contains("\"subject\":\"user\"").contains("\"admin\":true");
    }

    @Test
    void meWithoutAdminRoleIsNotAdmin() throws Exception {
        // given
        var request = get("/api/v1/me").with(oidcLogin());

        // when
        MvcResult result = mockMvc().perform(request).andReturn();

        // then
        int status = result.getResponse().getStatus();
        String body = result.getResponse().getContentAsString();
        assertThat(status).isEqualTo(200);
        assertThat(body).contains("\"admin\":false");
    }

    @Test
    void unsafeRequestWithoutCsrfTokenIsForbidden() throws Exception {
        // given
        var request = post("/logout").with(oidcLogin());

        // when
        MvcResult result = mockMvc().perform(request).andReturn();

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(403);
    }

    @Test
    void logoutWithCsrfTokenReturnsStatusAndClearsCookies() throws Exception {
        // given
        var request = post("/logout").secure(true).with(oidcLogin()).with(csrf());

        // when
        MvcResult result = mockMvc().perform(request).andReturn();

        // then
        int status = result.getResponse().getStatus();
        String clearSiteData = result.getResponse().getHeader("Clear-Site-Data");
        assertThat(status).isEqualTo(200);
        assertThat(clearSiteData).isEqualTo("\"cookies\"");
    }

    @Test
    void onlyPostLogoutIsHandled() throws Exception {
        // given
        var request = get("/logout").with(oidcLogin());

        // when
        MvcResult result = mockMvc().perform(request).andReturn();

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(404);
    }

    @Test
    void securityHeadersArePresentOnResponses() throws Exception {
        // when
        mockMvc()
                .perform(get("/healthz"))
                .andExpect(OK)
                .andExpect(CONTENT_SECURITY_POLICY)
                .andExpect(REFERRER_POLICY)
                .andExpect(PERMISSIONS_POLICY);
    }

    private static MockHttpSession authenticatedSession() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
}
