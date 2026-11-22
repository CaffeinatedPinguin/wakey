package dev.wakey.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.wakey.testsupport.SecurityIntegrationTest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

class SecurityFilterChainTest extends SecurityIntegrationTest {

    private static final ResultMatcher OK = status().isOk();

    private static final ResultMatcher UNAUTHORIZED = status().isUnauthorized();

    private static final ResultMatcher CONTENT_SECURITY_POLICY =
            header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");

    private static final ResultMatcher REFERRER_POLICY = header().string("Referrer-Policy", "no-referrer");

    private static final ResultMatcher PERMISSIONS_POLICY =
            header().string("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

    @Test
    void agentRequestsAreRejectedWithoutAuthentication() throws Exception {
        mockMvc().perform(get("/agent/v1/checks")).andExpect(UNAUTHORIZED);
    }

    @Test
    void pingRequestsAreRejectedWithoutAuthentication() throws Exception {
        mockMvc().perform(get("/ping/token")).andExpect(UNAUTHORIZED);
    }

    @Test
    void publicRequestsAreRejectedWithoutAuthentication() throws Exception {
        mockMvc().perform(get("/public/v1/status")).andExpect(UNAUTHORIZED);
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
}
