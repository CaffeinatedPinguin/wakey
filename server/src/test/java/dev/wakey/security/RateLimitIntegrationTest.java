package dev.wakey.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import dev.wakey.ratelimit.RateLimitProperties;
import dev.wakey.settings.SpacePlan;
import dev.wakey.settings.SpaceSettings;
import dev.wakey.spaces.PutResult;
import dev.wakey.spaces.Space;
import dev.wakey.spaces.SpaceMember;
import dev.wakey.spaces.SpaceMembers;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceService;
import dev.wakey.testsupport.SecurityIntegrationTest;
import dev.wakey.tokens.ApiTokens;
import dev.wakey.tokens.CreatedToken;
import dev.wakey.tokens.TokenOptions;
import dev.wakey.tokens.TokenOwner;
import dev.wakey.tokens.TokenScope;
import dev.wakey.web.CommonError;
import dev.wakey.web.CurrentUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(OutputCaptureExtension.class)
class RateLimitIntegrationTest extends SecurityIntegrationTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private static final String ISSUER = "https://issuer.example.com";

    private static final CurrentUser ADMIN = new CurrentUser(ISSUER, "fixture-admin", true);

    private static final String LOGIN_ATTEMPT_PATH = "/oauth2/authorization/oidc";

    private static final String PROBLEM_JSON = "application/problem+json";

    private static final int TOO_MANY_REQUESTS =
            CommonError.TOO_MANY_REQUESTS.getStatus().value();

    private CapturedOutput output;

    @BeforeEach
    void captureOutput(CapturedOutput capturedOutput) {
        this.output = capturedOutput;
    }

    @Test
    void tooManyLoginAttemptsGetTooManyRequestsWithRetryAfter() throws Exception {
        // given
        String address = "203.0.113.10";

        // when
        MvcResult rejected = rejectLoginAfter(address);
        JsonNode body = jsonOf(rejected);
        String code = body.path("code").asString();

        // then
        assertThat(statusOf(rejected)).isEqualTo(TOO_MANY_REQUESTS);
        assertThat(contentTypeOf(rejected)).isEqualTo(PROBLEM_JSON);
        assertThat(code).isEqualTo(CommonError.TOO_MANY_REQUESTS.getCode());
        assertThat(retryAfterSeconds(rejected)).isPositive();
        assertThat(output.getOut()).contains("login rate limit exceeded").contains(address);
    }

    @Test
    void loginAttemptsFromAnotherAddressAreNotLimited() throws Exception {
        // given
        String exhaustedAddress = "203.0.113.20";
        String otherAddress = "203.0.113.21";
        rejectLoginAfter(exhaustedAddress);

        // when
        MvcResult other = loginAttempt(otherAddress);

        // then
        assertThat(statusOf(other)).isNotEqualTo(TOO_MANY_REQUESTS);
    }

    @Test
    void anApiTokenOverItsPlanLimitGetsTooManyRequests() throws Exception {
        // given
        String subject = uniqueSubject("plan-limit");
        Space space = seedSpace("plan-limit", subject, "small");
        CreatedToken token = createToken(subject, space.slug());
        int limit = planApiLimit(space);
        List<Integer> allowed = new ArrayList<>();
        for (int request = 0; request < limit; request++) {
            MvcResult result = getSpaceWithToken(space.slug(), token);
            allowed.add(statusOf(result));
        }

        // when
        MvcResult rejected = getSpaceWithToken(space.slug(), token);
        JsonNode body = jsonOf(rejected);
        String code = body.path("code").asString();

        // then
        assertThat(allowed).containsOnly(200);
        assertThat(statusOf(rejected)).isEqualTo(TOO_MANY_REQUESTS);
        assertThat(contentTypeOf(rejected)).isEqualTo(PROBLEM_JSON);
        assertThat(code).isEqualTo(CommonError.TOO_MANY_REQUESTS.getCode());
        assertThat(retryAfterSeconds(rejected)).isPositive();
    }

    @Test
    void aBiggerPlanAllowsMoreRequests() throws Exception {
        // given
        String smallSubject = uniqueSubject("small-plan");
        String largeSubject = uniqueSubject("large-plan");
        Space small = seedSpace("small-plan", smallSubject, "small");
        Space large = seedSpace("large-plan", largeSubject, "large");
        CreatedToken smallToken = createToken(smallSubject, small.slug());
        CreatedToken largeToken = createToken(largeSubject, large.slug());
        int smallLimit = planApiLimit(small);
        int largeLimit = planApiLimit(large);
        assertThat(largeLimit).isGreaterThan(smallLimit);

        // when
        for (int request = 0; request < smallLimit; request++) {
            MvcResult result = getSpaceWithToken(small.slug(), smallToken);
            assertThat(statusOf(result)).isEqualTo(200);
        }
        MvcResult smallRejected = getSpaceWithToken(small.slug(), smallToken);
        for (int request = 0; request < largeLimit; request++) {
            MvcResult result = getSpaceWithToken(large.slug(), largeToken);
            assertThat(statusOf(result)).isEqualTo(200);
        }
        MvcResult largeRejected = getSpaceWithToken(large.slug(), largeToken);

        // then
        assertThat(statusOf(smallRejected)).isEqualTo(TOO_MANY_REQUESTS);
        assertThat(statusOf(largeRejected)).isEqualTo(TOO_MANY_REQUESTS);
    }

    @Test
    void browserSessionsAreNotLimitedByThePlan() throws Exception {
        // given
        String subject = uniqueSubject("browser-plan");
        Space space = seedSpace("browser-plan", subject, "small");
        int limit = planApiLimit(space);
        List<Integer> statuses = new ArrayList<>();

        // when
        for (int request = 0; request < limit + 2; request++) {
            MvcResult result = getSpaceWithSession(space.slug(), subject);
            statuses.add(statusOf(result));
        }

        // then
        assertThat(statuses).containsOnly(200);
    }

    private MvcResult rejectLoginAfter(String address) throws Exception {
        int capacity = bean(RateLimitProperties.class).login().capacity();
        for (int attempt = 0; attempt <= capacity + 1; attempt++) {
            MvcResult result = loginAttempt(address);
            if (statusOf(result) == TOO_MANY_REQUESTS) {
                return result;
            }
        }
        throw new AssertionError("Login attempts from " + address + " were not limited");
    }

    private MvcResult loginAttempt(String address) throws Exception {
        MockHttpServletRequestBuilder request = get(LOGIN_ATTEMPT_PATH).with(remoteAddress(address));
        return mockMvc().perform(request).andReturn();
    }

    private MvcResult getSpaceWithToken(String slug, CreatedToken token) throws Exception {
        MockHttpServletRequestBuilder request =
                get(spacePath(slug)).header(HttpHeaders.AUTHORIZATION, "Bearer " + token.secret());
        return mockMvc().perform(request).andReturn();
    }

    private MvcResult getSpaceWithSession(String slug, String subject) throws Exception {
        MockHttpServletRequestBuilder request = get(spacePath(slug)).with(login(subject));
        return mockMvc().perform(request).andReturn();
    }

    private Space seedSpace(String prefix, String subject, String plan) {
        SpaceService spaces = bean(SpaceService.class);
        String slug = uniqueSlug(prefix);
        PutResult created = spaces.put(ADMIN, slug, "Rate limit " + prefix, null);
        Space space = created.space();
        spaces.assignPlan(ADMIN, slug, plan);
        bean(SpaceMembers.class).addMember(ADMIN, slug, new SpaceMember(ISSUER, subject, SpaceRole.EDITOR));
        return space;
    }

    private CreatedToken createToken(String subject, String slug) {
        TokenOwner owner = new TokenOwner(ISSUER, subject);
        TokenOptions options = new TokenOptions(TokenScope.READ, null);
        return bean(ApiTokens.class).create(owner, slug, "token", options);
    }

    private int planApiLimit(Space space) {
        SpacePlan plan = bean(SpaceSettings.class).planOf(space.id());
        @Nullable Integer limit = plan.properties().apiRequestsPerMinute();
        return Objects.requireNonNull(limit);
    }

    private static RequestPostProcessor remoteAddress(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    private static RequestPostProcessor login(String subject) {
        return oidcLogin().idToken(token -> token.subject(subject).issuer(ISSUER));
    }

    private static String spacePath(String slug) {
        return "/api/v1/spaces/" + slug;
    }

    private static long retryAfterSeconds(MvcResult result) {
        String header = result.getResponse().getHeader(HttpHeaders.RETRY_AFTER);
        return Long.parseLong(Objects.requireNonNull(header));
    }

    private static int statusOf(MvcResult result) {
        return result.getResponse().getStatus();
    }

    private static String contentTypeOf(MvcResult result) {
        String contentType = result.getResponse().getContentType();
        return Objects.requireNonNull(contentType);
    }

    private static JsonNode jsonOf(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();
        return MAPPER.readTree(content);
    }

    private static String uniqueSlug(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String uniqueSubject(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
