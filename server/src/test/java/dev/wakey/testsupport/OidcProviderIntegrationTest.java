package dev.wakey.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.DockerImageName;

/** The full OIDC login against one containerised provider shared by the whole run. */
class OidcProviderIntegrationTest extends SecurityIntegrationTest {

    private static final String OIDC_IMAGE =
            "ghcr.io/navikt/mock-oauth2-server:7.0.1@sha256:eb07a0a65007c34c40c7b849592a47e6299128a514a5c6c582a53451cc6e3390";

    private static final String FRONTEND_URL = "https://frontend.wakey.test/after-login";

    private static final String OIDC_CONFIG = """
            {
              "interactiveLogin": false,
              "tokenCallbacks": [
                {
                  "issuerId": "default",
                  "requestMappings": [
                    {
                      "requestParam": "code",
                      "match": "*",
                      "claims": {
                        "sub": "admin-user",
                        "preferred_username": "admin-user",
                        "groups": ["wakey-admins"]
                      }
                    }
                  ]
                }
              ]
            }
            """;

    private static final WaitStrategy READY =
            Wait.forHttp("/default/.well-known/openid-configuration").forStatusCode(200);

    private static final GenericContainer<?> OIDC_PROVIDER = new GenericContainer<>(DockerImageName.parse(OIDC_IMAGE))
            .withExposedPorts(8080)
            .withEnv("JSON_CONFIG", OIDC_CONFIG)
            .waitingFor(READY);

    static {
        OIDC_PROVIDER.start();
    }

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void oidcProperties(DynamicPropertyRegistry registry) {
        String providerPort = String.valueOf(OIDC_PROVIDER.getMappedPort(8080));
        String issuer = "http://" + OIDC_PROVIDER.getHost() + ":" + providerPort + "/default";
        registry.add("spring.security.oauth2.client.registration.oidc.provider", () -> "oidc");
        registry.add("spring.security.oauth2.client.registration.oidc.client-id", () -> "wakey-test-client");
        registry.add("spring.security.oauth2.client.registration.oidc.client-secret", () -> "wakey-test-secret");
        registry.add("spring.security.oauth2.client.registration.oidc.scope", () -> "openid,profile,email");
        registry.add("spring.security.oauth2.client.provider.oidc.issuer-uri", () -> issuer);
        registry.add("wakey.security.admin-group", () -> "wakey-admins");
        registry.add("wakey.security.frontend-url", () -> FRONTEND_URL);
    }

    @Test
    void adminGroupFromProviderCreatesSessionAndReportsAdmin() throws Exception {
        // given
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        String baseUrl = "http://localhost:" + port;
        ClientRegistrationRepository registrations = bean(ClientRegistrationRepository.class);
        ClientRegistration registration = Objects.requireNonNull(registrations.findByRegistrationId("oidc"));

        // when
        HttpResponse<String> authorization = send(client, baseUrl + "/oauth2/authorization/oidc", null);

        // then
        assertThat(authorization.statusCode()).isEqualTo(HttpStatus.FOUND.value());
        boolean proofKey = registration.getClientSettings().isRequireProofKey();
        assertThat(proofKey).isTrue();
        String authorizationCookie = sessionSetCookie(authorization);
        assertThat(authorizationCookie).contains("HttpOnly", "Secure", "SameSite=Lax");
        String authorizationLocation = location(authorization);
        assertThat(authorizationLocation).contains("/default/authorize");

        // when
        HttpResponse<String> provider = send(client, authorizationLocation, null);
        String providerLocation = location(provider);

        // then
        assertThat(provider.statusCode()).isEqualTo(HttpStatus.FOUND.value());
        assertThat(providerLocation).contains("/login/oauth2/code/oidc?code=");

        // when
        HttpResponse<String> callback = send(client, providerLocation, cookiePair(authorizationCookie));
        String callbackCookie = sessionSetCookieOrFallback(callback, authorizationCookie);

        // then
        assertThat(callback.statusCode()).isEqualTo(HttpStatus.FOUND.value());

        // when
        HttpResponse<String> me = send(client, baseUrl + "/api/v1/me", cookiePair(callbackCookie));

        // then
        assertThat(me.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(me.body()).contains("\"subject\":\"admin-user\"").contains("\"admin\":true");
        long sessions = jdbc().sql("SELECT COUNT(*) FROM SPRING_SESSION WHERE PRINCIPAL_NAME = 'admin-user'")
                .query(Long.class)
                .single();
        assertThat(sessions).isEqualTo(1L);
    }

    @Test
    void loginAlwaysReturnsToTheFrontendUrl() throws Exception {
        // given
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        String baseUrl = "http://localhost:" + port;
        String authorizationUrl = baseUrl + "/oauth2/authorization/oidc?redirect=https://evil.example/steal";

        // when
        HttpResponse<String> authorization = send(client, authorizationUrl, null);
        String authorizationCookie = sessionSetCookie(authorization);
        HttpResponse<String> provider = send(client, location(authorization), null);
        HttpResponse<String> callback = send(client, location(provider), cookiePair(authorizationCookie));

        // then
        String redirect = location(callback);
        assertThat(redirect).isEqualTo(FRONTEND_URL).doesNotContain("evil.example");
    }

    @Test
    void failedCallbackRedirectsWithoutDetails() throws Exception {
        // given
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        String baseUrl = "http://localhost:" + port;
        HttpResponse<String> authorization = send(client, baseUrl + "/oauth2/authorization/oidc", null);
        String cookie = cookiePair(sessionSetCookie(authorization));
        String forgedCallback = baseUrl + "/login/oauth2/code/oidc?code=forged-code&state=forged-state";

        // when
        HttpResponse<String> callback = send(client, forgedCallback, cookie);

        // then
        String redirect = location(callback);
        assertThat(callback.statusCode()).isEqualTo(HttpStatus.FOUND.value());
        assertThat(redirect).isEqualTo(FRONTEND_URL + "?login=failed").doesNotContain("forged");
        assertThat(callback.body()).doesNotContain("forged", "invalid_state", "state=");

        // when
        HttpResponse<String> me = send(client, baseUrl + "/api/v1/me", cookie);

        // then
        assertThat(me.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void getLogoutDoesNotRenderTheGeneratedPage() throws Exception {
        // given
        var request = get("/logout").with(oidcLogin());

        // when
        MvcResult result = mockMvc().perform(request).andReturn();

        // then
        int status = result.getResponse().getStatus();
        String body = result.getResponse().getContentAsString();
        assertThat(status).isNotIn(200, 204, 302);
        assertThat(body).doesNotContain("Are you sure you want to log out?");
    }

    private static HttpResponse<String> send(HttpClient client, String url, @Nullable String cookie)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).GET();
        if (cookie != null) {
            builder.header("Cookie", cookie);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String location(HttpResponse<String> response) {
        return response.headers().firstValue("Location").orElseThrow();
    }

    private static String sessionSetCookie(HttpResponse<String> response) {
        return response.headers().allValues("Set-Cookie").stream()
                .filter(header -> header.startsWith("SESSION="))
                .findFirst()
                .orElseThrow();
    }

    private static String sessionSetCookieOrFallback(HttpResponse<String> response, String fallback) {
        return response.headers().allValues("Set-Cookie").stream()
                .filter(header -> header.startsWith("SESSION="))
                .findFirst()
                .orElse(fallback);
    }

    private static String cookiePair(String setCookie) {
        return setCookie.split(";", 2)[0];
    }
}
