package dev.wakey.api.tokens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import dev.wakey.spaces.PutResult;
import dev.wakey.spaces.Space;
import dev.wakey.spaces.SpacePrincipal;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceService;
import dev.wakey.testsupport.SecurityIntegrationTest;
import dev.wakey.tokens.ApiTokens;
import dev.wakey.tokens.CreatedToken;
import dev.wakey.tokens.TokenOptions;
import dev.wakey.tokens.TokenOwner;
import dev.wakey.tokens.TokenScope;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class TokensHttpIntegrationTest extends SecurityIntegrationTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private static final SpacePrincipal ADMIN = new SpacePrincipal("fixture-admin", true);

    private static final String SPACES = "/api/v1/spaces";

    private static final String TOKENS = "/tokens";

    private static final String PROBLEM_JSON = "application/problem+json";

    private static final String WRITE_CALL = "/api/v1/test-write";

    @Test
    void aReadTokenReadsItsSpace() throws Exception {
        // given
        String subject = uniqueSubject("read-token");
        Space space = seedSpace("read", subject, SpaceRole.EDITOR);
        CreatedToken token = createToken(subject, space.slug(), TokenScope.READ);
        String path = SPACES + "/" + space.slug();

        // when
        MvcResult result = performWithToken(get(path), token);

        // then
        JsonNode body = jsonOf(result);
        String slug = body.path("slug").asString();
        String role = body.path("role").asString();
        assertThat(statusOf(result)).isEqualTo(200);
        assertThat(slug).isEqualTo(space.slug());
        assertThat(role).isEqualTo("EDITOR");
    }

    @Test
    void aReadTokenCannotWrite() throws Exception {
        // given
        String subject = uniqueSubject("read-write");
        Space space = seedSpace("read-write", subject, SpaceRole.EDITOR);
        CreatedToken token = createToken(subject, space.slug(), TokenScope.READ);

        // when
        String slug = space.slug();
        MvcResult result = performWithToken(renameRequest(slug, "Renamed"), token);

        // then
        JsonNode body = jsonOf(result);
        String code = body.path("code").asString();
        assertThat(statusOf(result)).isEqualTo(403);
        assertThat(contentTypeOf(result)).isEqualTo(PROBLEM_JSON);
        assertThat(code).isEqualTo("insufficient_scope");
    }

    @Test
    void aWriteTokenCanWriteWithoutACsrfToken() throws Exception {
        // given
        String subject = uniqueSubject("write-token");
        Space space = seedSpace("write", subject, SpaceRole.EDITOR);
        CreatedToken token = createToken(subject, space.slug(), TokenScope.WRITE);

        // when
        MvcResult result = performWithToken(post(WRITE_CALL), token);

        // then
        assertThat(statusOf(result)).isEqualTo(200);
    }

    @Test
    void aWriteTokenOfAnEditorChangesDomainData() throws Exception {
        // given
        String subject = uniqueSubject("write-domain");
        Space space = seedSpace("write-domain", subject, SpaceRole.EDITOR);
        CreatedToken writeToken = createToken(subject, space.slug(), TokenScope.WRITE);
        CreatedToken readToken = createToken(subject, space.slug(), TokenScope.READ);
        String settingsPath = SPACES + "/" + space.slug() + "/settings";
        String intervalPath = settingsPath + "/check.interval";

        // when
        MvcResult written = performWithToken(settingRequest(intervalPath, "45"), writeToken);
        MvcResult rejected = performWithToken(settingRequest(intervalPath, "90"), readToken);
        MvcResult settings = performWithToken(get(settingsPath), writeToken);

        // then
        JsonNode interval = settingOf(jsonOf(settings), "check.interval");
        String rejectedCode = jsonOf(rejected).path("code").asString();
        String storedValue = interval.path("value").asString();
        String source = interval.path("source").asString();
        assertThat(statusOf(written)).isEqualTo(204);
        assertThat(statusOf(rejected)).isEqualTo(403);
        assertThat(rejectedCode).isEqualTo("insufficient_scope");
        assertThat(storedValue).isEqualTo("45");
        assertThat(source).isEqualTo("SPACE");
    }

    @Test
    void aTokenOfAnotherSpaceIsNotFound() throws Exception {
        // given
        String subject = uniqueSubject("two-spaces");
        Space own = seedSpace("own", subject, SpaceRole.EDITOR);
        Space other = seedSpace("other", subject, SpaceRole.EDITOR);
        CreatedToken token = createToken(subject, own.slug(), TokenScope.READ);
        String path = SPACES + "/" + other.slug();

        // when
        MvcResult result = performWithToken(get(path), token);

        // then
        JsonNode body = jsonOf(result);
        String code = body.path("code").asString();
        assertThat(statusOf(result)).isEqualTo(404);
        assertThat(contentTypeOf(result)).isEqualTo(PROBLEM_JSON);
        assertThat(code).isEqualTo("not_found");
    }

    @Test
    void theSpaceListOfATokenHasOnlyItsSpace() throws Exception {
        // given
        String subject = uniqueSubject("token-list");
        Space own = seedSpace("token-own", subject, SpaceRole.EDITOR);
        Space other = seedSpace("token-other", subject, SpaceRole.EDITOR);
        CreatedToken token = createToken(subject, own.slug(), TokenScope.READ);

        // when
        MvcResult result = performWithToken(get(SPACES), token);

        // then
        JsonNode body = jsonOf(result);
        List<String> slugs = slugsOf(body);
        assertThat(statusOf(result)).isEqualTo(200);
        assertThat(slugs).containsExactly(own.slug()).doesNotContain(other.slug());
    }

    @Test
    void anExpiredTokenIsRejected() throws Exception {
        // given
        String subject = uniqueSubject("expired-token");
        Space space = seedSpace("expired", subject, SpaceRole.EDITOR);
        Clock clock = bean(Clock.class);
        Instant past = Instant.now(clock).minusSeconds(60);
        CreatedToken token = createToken(subject, space.slug(), TokenScope.READ, past);

        // when
        MvcResult result = performWithToken(get("/api/v1/me"), token);

        // then
        JsonNode body = jsonOf(result);
        String code = body.path("code").asString();
        assertThat(statusOf(result)).isEqualTo(401);
        assertThat(code).isEqualTo("invalid_token");
    }

    @Test
    void aRevokedTokenIsRejected() throws Exception {
        // given
        String subject = uniqueSubject("revoked-token");
        Space space = seedSpace("revoked", subject, SpaceRole.EDITOR);
        CreatedToken token = createToken(subject, space.slug(), TokenScope.WRITE);
        String itemPath = tokenItemPath(space.slug(), token);
        MockHttpServletRequestBuilder revoke =
                delete(itemPath).with(login(subject)).with(csrf());

        // when
        MvcResult revokeResult = mockMvc().perform(revoke).andReturn();
        MvcResult useResult = performWithToken(get("/api/v1/me"), token);

        // then
        JsonNode body = jsonOf(useResult);
        String code = body.path("code").asString();
        assertThat(statusOf(revokeResult)).isEqualTo(204);
        assertThat(statusOf(useResult)).isEqualTo(401);
        assertThat(code).isEqualTo("invalid_token");
    }

    @Test
    void aTokenWithAWrongPrefixIsRejected() throws Exception {
        // given
        String subject = uniqueSubject("wrong-prefix");
        Space space = seedSpace("wrong-prefix", subject, SpaceRole.EDITOR);
        CreatedToken token = createToken(subject, space.slug(), TokenScope.READ);
        String wrongSecret = "wka_" + token.secret();
        MockHttpServletRequestBuilder request =
                get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + wrongSecret);

        // when
        MvcResult result = mockMvc().perform(request).andReturn();

        // then
        JsonNode body = jsonOf(result);
        String code = body.path("code").asString();
        assertThat(statusOf(result)).isEqualTo(401);
        assertThat(code).isEqualTo("invalid_token");
    }

    @Test
    void aValidSessionDoesNotRescueABadToken() throws Exception {
        // given
        MockHttpSession session = authenticatedSession();
        MockHttpServletRequestBuilder request =
                get("/api/v1/me").session(session).header(HttpHeaders.AUTHORIZATION, "Bearer wk_not-a-real-token");

        // when
        MvcResult result = mockMvc().perform(request).andReturn();

        // then
        JsonNode body = jsonOf(result);
        String code = body.path("code").asString();
        HttpSession resultSession = result.getRequest().getSession(false);
        assertThat(resultSession).isNotNull();
        assertThat(statusOf(result)).isEqualTo(401);
        assertThat(code).isEqualTo("invalid_token");
    }

    @Test
    void agentPublicAndPingSurfacesRejectBearerTokens() throws Exception {
        // given
        String subject = uniqueSubject("surface");
        Space space = seedSpace("surface", subject, SpaceRole.EDITOR);
        CreatedToken token = createToken(subject, space.slug(), TokenScope.WRITE);
        List<String> paths = List.of("/agent/v1/checks", "/public/v1/status", "/ping/token");
        List<Integer> statuses = new ArrayList<>();

        // when
        for (String path : paths) {
            MvcResult result = performWithToken(get(path), token);
            statuses.add(statusOf(result));
        }

        // then
        assertThat(statuses).containsOnly(401);
    }

    @Test
    void theSecretIsReturnedOnlyOnCreation() throws Exception {
        // given
        String subject = uniqueSubject("secret");
        Space space = seedSpace("secret", subject, SpaceRole.EDITOR);
        MockHttpServletRequestBuilder create = post(SPACES + "/" + space.slug() + TOKENS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(tokenBody("ci-deploy", TokenScope.READ))
                .with(login(subject))
                .with(csrf());

        // when
        MvcResult created = mockMvc().perform(create).andReturn();
        String collectionPath = SPACES + "/" + space.slug() + TOKENS;
        MockHttpServletRequestBuilder listRequest = get(collectionPath).with(login(subject));
        MvcResult listed = mockMvc().perform(listRequest).andReturn();

        // then
        JsonNode createdBody = jsonOf(created);
        String secret = createdBody.path("secret").asString();
        String listedBody = listed.getResponse().getContentAsString();
        assertThat(statusOf(created)).isEqualTo(201);
        assertThat(secret).startsWith("wk_");
        assertThat(statusOf(listed)).isEqualTo(200);
        assertThat(listedBody).doesNotContain(secret);
        assertThat(listedBody).doesNotContain("token_hash");
    }

    @Test
    void aTokenCannotManageTokens() throws Exception {
        // given
        String subject = uniqueSubject("manage-tokens");
        Space space = seedSpace("manage", subject, SpaceRole.EDITOR);
        CreatedToken token = createToken(subject, space.slug(), TokenScope.WRITE);
        String collectionPath = SPACES + "/" + space.slug() + TOKENS;
        MockHttpServletRequestBuilder create = post(collectionPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(tokenBody("from-token", TokenScope.READ));

        // when
        MvcResult createResult = performWithToken(create, token);
        MvcResult listResult = performWithToken(get(collectionPath), token);

        // then
        String createCode = jsonOf(createResult).path("code").asString();
        String listCode = jsonOf(listResult).path("code").asString();
        assertThat(statusOf(createResult)).isEqualTo(403);
        assertThat(createCode).isEqualTo("insufficient_scope");
        assertThat(statusOf(listResult)).isEqualTo(403);
        assertThat(listCode).isEqualTo("insufficient_scope");
    }

    @Test
    void aTokenCannotManageTokensThroughAPercentEncodedPath() throws Exception {
        // given
        String subject = uniqueSubject("encoded-tokens");
        Space space = seedSpace("encoded", subject, SpaceRole.EDITOR);
        CreatedToken writeToken = createToken(subject, space.slug(), TokenScope.WRITE);
        CreatedToken readToken = createToken(subject, space.slug(), TokenScope.READ);
        URI encodedCollection = encodedTokensPath(space.slug());

        // when
        MockHttpServletRequestBuilder writeListRequest = get("/").uri(encodedCollection);
        MockHttpServletRequestBuilder readListRequest = get("/").uri(encodedCollection);
        MvcResult writeCreate = performWithToken(tokenCreateRequest(encodedCollection), writeToken);
        MvcResult readCreate = performWithToken(tokenCreateRequest(encodedCollection), readToken);
        MvcResult writeList = performWithToken(writeListRequest, writeToken);
        MvcResult readList = performWithToken(readListRequest, readToken);
        MockHttpServletRequestBuilder listRequest =
                get(SPACES + "/" + space.slug() + TOKENS).with(login(subject));
        MvcResult listed = mockMvc().perform(listRequest).andReturn();

        // then
        String listedBody = listed.getResponse().getContentAsString();
        assertThat(statusOf(writeCreate)).isEqualTo(403);
        assertThat(codeOf(writeCreate)).isEqualTo("insufficient_scope");
        assertThat(statusOf(readCreate)).isEqualTo(403);
        assertThat(codeOf(readCreate)).isEqualTo("insufficient_scope");
        assertThat(statusOf(writeList)).isEqualTo(403);
        assertThat(codeOf(writeList)).isEqualTo("insufficient_scope");
        assertThat(statusOf(readList)).isEqualTo(403);
        assertThat(codeOf(readList)).isEqualTo("insufficient_scope");
        assertThat(statusOf(listed)).isEqualTo(200);
        assertThat(listedBody).doesNotContain("from-encoded");
    }

    private static String codeOf(MvcResult result) throws Exception {
        JsonNode body = jsonOf(result);
        return body.path("code").asString();
    }

    @Test
    void loweringTheRoleLowersTheTokenOverHttp() throws Exception {
        // given
        String subject = uniqueSubject("lower-token");
        Space space = seedSpace("lower", subject, SpaceRole.EDITOR);
        CreatedToken token = createToken(subject, space.slug(), TokenScope.WRITE);

        // when
        MvcResult before = performWithToken(post(WRITE_CALL), token);
        spaces().changeRole(ADMIN, space.slug(), subject, SpaceRole.VIEWER);
        MvcResult after = performWithToken(post(WRITE_CALL), token);

        // then
        String code = jsonOf(after).path("code").asString();
        assertThat(statusOf(before)).isEqualTo(200);
        assertThat(statusOf(after)).isEqualTo(403);
        assertThat(code).isEqualTo("insufficient_scope");
    }

    private MvcResult performWithToken(MockHttpServletRequestBuilder request, CreatedToken token) throws Exception {
        MockHttpServletRequestBuilder authenticated =
                request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.secret());
        return mockMvc().perform(authenticated).andReturn();
    }

    private static MockHttpServletRequestBuilder renameRequest(String slug, String name) {
        String path = SPACES + "/" + slug;
        String body = "{\"name\":\"" + name + "\"}";
        return put(path).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static MockHttpServletRequestBuilder settingRequest(String path, String value) {
        String body = "{\"value\":\"" + value + "\"}";
        return put(path).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static JsonNode settingOf(JsonNode settings, String key) {
        JsonNode found = null;
        for (JsonNode item : settings) {
            String itemKey = item.path("key").asString();
            if (key.equals(itemKey)) {
                found = item;
            }
        }
        return Objects.requireNonNull(found, "Missing setting " + key);
    }

    private CreatedToken createToken(String subject, String slug, TokenScope scope) {
        return createToken(subject, slug, scope, null);
    }

    private CreatedToken createToken(String subject, String slug, TokenScope scope, @Nullable Instant expiresAt) {
        ApiTokens tokens = bean(ApiTokens.class);
        TokenOwner owner = new TokenOwner(subject);
        TokenOptions options = new TokenOptions(scope, expiresAt);
        return tokens.create(owner, slug, "token", options);
    }

    private Space seedSpace(String prefix, String subject, SpaceRole role) {
        SpaceService service = bean(SpaceService.class);
        String slug = uniqueSlug(prefix);
        PutResult created = service.put(ADMIN, slug, "Space " + prefix);
        Space space = created.space();
        service.addMember(ADMIN, space.slug(), subject, role);
        return space;
    }

    private SpaceService spaces() {
        return bean(SpaceService.class);
    }

    private static String tokenItemPath(String slug, CreatedToken token) {
        String tokenId = token.token().id().toString();
        return SPACES + "/" + slug + TOKENS + "/" + tokenId;
    }

    private static String tokenBody(String name, TokenScope scope) {
        return "{\"name\":\"" + name + "\",\"scope\":\"" + scope.name() + "\"}";
    }

    private static MockHttpServletRequestBuilder tokenCreateRequest(URI encodedCollection) {
        return post("/")
                .uri(encodedCollection)
                .contentType(MediaType.APPLICATION_JSON)
                .content(tokenBody("from-encoded", TokenScope.READ));
    }

    private static URI encodedTokensPath(String slug) {
        return URI.create(SPACES + "/" + slug + "/t%6Fkens");
    }

    private static RequestPostProcessor login(String subject) {
        return oidcLogin().idToken(token -> token.subject(subject));
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

    private static List<String> slugsOf(JsonNode page) {
        List<String> slugs = new ArrayList<>();
        for (JsonNode item : page.path("items")) {
            String slug = item.path("slug").asString();
            slugs.add(slug);
        }
        return slugs;
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
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return prefix + "-" + suffix;
    }

    private static String uniqueSubject(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    @TestConfiguration
    static class WriteEndpointConfiguration {

        @Bean
        WriteEndpoint writeEndpoint() {
            return new WriteEndpoint();
        }
    }

    @RestController
    static final class WriteEndpoint {

        @PostMapping(WRITE_CALL)
        Map<String, String> write() {
            return Map.of("status", "written");
        }
    }
}
