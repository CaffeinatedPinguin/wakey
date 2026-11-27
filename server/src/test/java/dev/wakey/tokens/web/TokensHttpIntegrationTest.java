package dev.wakey.tokens.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import dev.wakey.spaces.PutResult;
import dev.wakey.spaces.Space;
import dev.wakey.spaces.SpaceMember;
import dev.wakey.spaces.SpaceMemberId;
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
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.RecordComponent;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
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

@ExtendWith(OutputCaptureExtension.class)
class TokensHttpIntegrationTest extends SecurityIntegrationTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private static final String ISSUER = "https://issuer.example.com";

    private static final CurrentUser ADMIN = new CurrentUser(ISSUER, "fixture-admin", true);

    private static final String SPACES = "/api/v1/spaces";

    private static final String TOKENS = "/tokens";

    private static final String PROBLEM_JSON = "application/problem+json";

    private static final String WRITE_CALL = "/api/v1/test-write";

    private CapturedOutput output;

    @BeforeEach
    void captureOutput(CapturedOutput capturedOutput) {
        this.output = capturedOutput;
    }

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
        assertThat(role).isEqualTo(SpaceRole.EDITOR.name());
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
        assertThat(code).isEqualTo(TokenError.INSUFFICIENT_SCOPE.getCode());
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
        assertThat(rejectedCode).isEqualTo(TokenError.INSUFFICIENT_SCOPE.getCode());
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
        assertThat(code).isEqualTo(CommonError.NOT_FOUND.getCode());
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
        CreatedToken token = createToken(subject, space.slug(), TokenScope.READ);
        UUID tokenId = token.token().id();
        expireAt(tokenId, past);

        // when
        MvcResult result = performWithToken(get("/api/v1/me"), token);

        // then
        JsonNode body = jsonOf(result);
        String code = body.path("code").asString();
        assertThat(statusOf(result)).isEqualTo(401);
        assertThat(code).isEqualTo(TokenError.INVALID_TOKEN.getCode());
    }

    @Test
    void aReinvitedMemberDoesNotGetTheirOldTokensBack() throws Exception {
        // given
        String subject = uniqueSubject("re-invited");
        Space space = seedSpace("re-invited", subject, SpaceRole.EDITOR);
        CreatedToken old = createToken(subject, space.slug(), TokenScope.WRITE);

        // when
        MvcResult beforeRemoval = performWithToken(get("/api/v1/me"), old);
        members().removeMember(ADMIN, space.slug(), new SpaceMemberId(ISSUER, subject));
        members().addMember(ADMIN, space.slug(), new SpaceMember(ISSUER, subject, SpaceRole.EDITOR));
        MvcResult afterReInvite = performWithToken(get("/api/v1/me"), old);
        CreatedToken fresh = createToken(subject, space.slug(), TokenScope.WRITE);
        MvcResult freshUse = performWithToken(get("/api/v1/me"), fresh);

        // then
        assertThat(statusOf(beforeRemoval)).isEqualTo(200);
        assertThat(statusOf(afterReInvite)).isEqualTo(401);
        assertThat(codeOf(afterReInvite)).isEqualTo(TokenError.INVALID_TOKEN.getCode());
        assertThat(statusOf(freshUse)).isEqualTo(200);
    }

    @Test
    void aRejectedTokenIsLoggedWithAClosedReason() throws Exception {
        // given
        String subject = uniqueSubject("closed-reason");
        Space space = seedSpace("closed-reason", subject, SpaceRole.EDITOR);
        CreatedToken readToken = createToken(subject, space.slug(), TokenScope.READ);
        String invalidSecret = "wk_" + UUID.randomUUID();

        // when
        MockHttpServletRequestBuilder invalidRequest =
                get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidSecret);
        MvcResult invalid = mockMvc().perform(invalidRequest).andReturn();
        MvcResult insufficient = performWithToken(post(WRITE_CALL), readToken);

        // then
        assertThat(statusOf(invalid)).isEqualTo(401);
        assertThat(statusOf(insufficient)).isEqualTo(403);
        assertThat(logEntries("reason=invalid-token")).hasSize(1);
        assertThat(logEntries("reason=insufficient-scope")).hasSize(1);
        assertThat(output.getOut()).doesNotContain(readToken.secret()).doesNotContain(invalidSecret);
    }

    @Test
    void aRejectedTokenResponseHasWwwAuthenticate() throws Exception {
        // given
        String subject = uniqueSubject("challenge");
        Space space = seedSpace("challenge", subject, SpaceRole.EDITOR);
        CreatedToken readToken = createToken(subject, space.slug(), TokenScope.READ);

        // when
        MockHttpServletRequestBuilder invalidRequest =
                get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer wk_not-a-real-token");
        MvcResult invalid = mockMvc().perform(invalidRequest).andReturn();
        MvcResult insufficient = performWithToken(post(WRITE_CALL), readToken);

        // then
        assertThat(statusOf(invalid)).isEqualTo(401);
        assertThat(headerOf(invalid, HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer error=\"invalid_token\"");
        assertThat(statusOf(insufficient)).isEqualTo(403);
        assertThat(headerOf(insufficient, HttpHeaders.WWW_AUTHENTICATE))
                .isEqualTo("Bearer error=\"insufficient_scope\"");
    }

    @Test
    void anExpiryInThePastIsRejected() throws Exception {
        // given
        String subject = uniqueSubject("past-expiry");
        Space space = seedSpace("past-expiry", subject, SpaceRole.EDITOR);
        Instant past = Instant.now(bean(Clock.class)).minusSeconds(60);
        String body = "{\"name\":\"past\",\"scope\":\"READ\",\"expiresAt\":\"" + past + "\"}";
        MockHttpServletRequestBuilder create = post(SPACES + "/" + space.slug() + TOKENS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(login(subject))
                .with(csrf());

        // when
        MvcResult result = mockMvc().perform(create).andReturn();

        // then
        JsonNode bodyNode = jsonOf(result);
        String code = bodyNode.path("code").asString();
        String pointer = bodyNode.path("errors").path(0).path("pointer").asString();
        assertThat(statusOf(result)).isEqualTo(400);
        assertThat(code).isEqualTo(CommonError.VALIDATION_FAILED.getCode());
        assertThat(pointer).isEqualTo("/expiresAt");
    }

    @Test
    void aTokenLimitPerOwnerIsEnforced() throws Exception {
        // given
        String subject = uniqueSubject("limit");
        Space space = seedSpace("limit", subject, SpaceRole.EDITOR);
        for (int index = 0; index < 50; index++) {
            createToken(subject, space.slug(), TokenScope.READ);
        }
        MockHttpServletRequestBuilder create = post(SPACES + "/" + space.slug() + TOKENS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(tokenBody("over-limit", TokenScope.READ))
                .with(login(subject))
                .with(csrf());

        // when
        MvcResult result = mockMvc().perform(create).andReturn();

        // then
        JsonNode body = jsonOf(result);
        String code = body.path("code").asString();
        int bound = body.path("params").path("bound").asInt();
        assertThat(statusOf(result)).isEqualTo(400);
        assertThat(code).isEqualTo(TokenError.TOKEN_LIMIT.getCode());
        assertThat(bound).isEqualTo(50);
    }

    @Test
    void currentUserHasNoTokenContext() throws Exception {
        // given
        String subject = uniqueSubject("me-context");
        Space space = seedSpace("me-context", subject, SpaceRole.EDITOR);
        CreatedToken token = createToken(subject, space.slug(), TokenScope.READ);

        // when
        MvcResult result = performWithToken(get("/api/v1/me"), token);

        // then
        JsonNode body = jsonOf(result);
        List<String> fields = new ArrayList<>();
        body.propertyNames().forEach(fields::add);
        List<String> components = new ArrayList<>();
        for (RecordComponent component : CurrentUser.class.getRecordComponents()) {
            components.add(component.getName());
        }
        assertThat(statusOf(result)).isEqualTo(200);
        assertThat(components).containsExactly("issuer", "subject", "admin");
        assertThat(fields).containsExactlyInAnyOrder("issuer", "subject", "admin");
    }

    @Test
    void aTokenAuthenticatesAsItsOwnerWithIssuer() throws Exception {
        // given
        String subject = uniqueSubject("owner-issuer");
        String tokenIssuer = "https://token-owner.example.com";
        PutResult created = spaces().put(ADMIN, uniqueSlug("owner-issuer"), "Owner issuer", null);
        Space space = created.space();
        members().addMember(ADMIN, space.slug(), new SpaceMember(tokenIssuer, subject, SpaceRole.EDITOR));
        TokenOwner owner = new TokenOwner(tokenIssuer, subject);
        CreatedToken token =
                bean(ApiTokens.class).create(owner, space.slug(), "owner", new TokenOptions(TokenScope.READ, null));

        // when
        MvcResult result = performWithToken(get("/api/v1/me"), token);

        // then
        JsonNode body = jsonOf(result);
        String issuerValue = body.path("issuer").asString();
        String subjectValue = body.path("subject").asString();
        boolean adminValue = body.path("admin").asBoolean();
        assertThat(statusOf(result)).isEqualTo(200);
        assertThat(issuerValue).isEqualTo(tokenIssuer);
        assertThat(subjectValue).isEqualTo(subject);
        assertThat(adminValue).isFalse();
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
        assertThat(code).isEqualTo(TokenError.INVALID_TOKEN.getCode());
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
        assertThat(code).isEqualTo(TokenError.INVALID_TOKEN.getCode());
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
        assertThat(code).isEqualTo(TokenError.INVALID_TOKEN.getCode());
    }

    @Test
    void aBadTokenIsRejectedEvenWithAValidSession() throws Exception {
        // given
        MockHttpSession session = authenticatedSession();
        MockHttpServletRequestBuilder request =
                get("/api/v1/me").session(session).header(HttpHeaders.AUTHORIZATION, "Bearer wk_not-a-real-token");

        // when
        MvcResult result = mockMvc().perform(request).andReturn();

        // then
        HttpSession resultSession = result.getRequest().getSession(false);
        assertThat(statusOf(result)).isEqualTo(401);
        assertThat(codeOf(result)).isEqualTo(TokenError.INVALID_TOKEN.getCode());
        assertThat(headerOf(result, HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer error=\"invalid_token\"");
        assertThat(resultSession).isNotNull();
    }

    @Test
    void aReadTokenCannotWriteAndGetsInsufficientScope() throws Exception {
        // given
        String subject = uniqueSubject("scope-rule");
        Space space = seedSpace("scope-rule", subject, SpaceRole.EDITOR);
        CreatedToken token = createToken(subject, space.slug(), TokenScope.READ);

        // when
        MvcResult result = performWithToken(post(WRITE_CALL), token);

        // then
        assertThat(statusOf(result)).isEqualTo(403);
        assertThat(codeOf(result)).isEqualTo(TokenError.INSUFFICIENT_SCOPE.getCode());
        assertThat(headerOf(result, HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer error=\"insufficient_scope\"");
    }

    @Test
    void aTokenRequestSkipsCsrf() throws Exception {
        // given
        String subject = uniqueSubject("token-csrf");
        Space space = seedSpace("token-csrf", subject, SpaceRole.EDITOR);
        CreatedToken token = createToken(subject, space.slug(), TokenScope.WRITE);

        // when
        MvcResult result = performWithToken(post(WRITE_CALL), token);

        // then
        assertThat(statusOf(result)).isEqualTo(200);
    }

    @Test
    void aMissingBearerHeaderFallsBackToTheSession() throws Exception {
        // given
        String subject = uniqueSubject("session-fallback");
        MockHttpServletRequestBuilder request = get("/api/v1/me").with(login(subject));

        // when
        MvcResult result = mockMvc().perform(request).andReturn();

        // then
        assertThat(statusOf(result)).isEqualTo(200);
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
        assertThat(createCode).isEqualTo(TokenError.INSUFFICIENT_SCOPE.getCode());
        assertThat(statusOf(listResult)).isEqualTo(403);
        assertThat(listCode).isEqualTo(TokenError.INSUFFICIENT_SCOPE.getCode());
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
        assertThat(codeOf(writeCreate)).isEqualTo(TokenError.INSUFFICIENT_SCOPE.getCode());
        assertThat(statusOf(readCreate)).isEqualTo(403);
        assertThat(codeOf(readCreate)).isEqualTo(TokenError.INSUFFICIENT_SCOPE.getCode());
        assertThat(statusOf(writeList)).isEqualTo(403);
        assertThat(codeOf(writeList)).isEqualTo(TokenError.INSUFFICIENT_SCOPE.getCode());
        assertThat(statusOf(readList)).isEqualTo(403);
        assertThat(codeOf(readList)).isEqualTo(TokenError.INSUFFICIENT_SCOPE.getCode());
        assertThat(statusOf(listed)).isEqualTo(200);
        assertThat(listedBody).doesNotContain("from-encoded");
    }

    private static String codeOf(MvcResult result) throws Exception {
        JsonNode body = jsonOf(result);
        return body.path("code").asString();
    }

    private List<String> logEntries(String fragment) {
        return output.getOut().lines().filter(line -> line.contains(fragment)).toList();
    }

    private static String headerOf(MvcResult result, String name) {
        String header = result.getResponse().getHeader(name);
        return Objects.requireNonNull(header);
    }

    @Test
    void loweringTheRoleLowersTheTokenOverHttp() throws Exception {
        // given
        String subject = uniqueSubject("lower-token");
        Space space = seedSpace("lower", subject, SpaceRole.EDITOR);
        CreatedToken token = createToken(subject, space.slug(), TokenScope.WRITE);

        // when
        MvcResult before = performWithToken(post(WRITE_CALL), token);
        members().changeRole(ADMIN, space.slug(), new SpaceMember(ISSUER, subject, SpaceRole.VIEWER));
        MvcResult after = performWithToken(post(WRITE_CALL), token);

        // then
        String code = jsonOf(after).path("code").asString();
        assertThat(statusOf(before)).isEqualTo(200);
        assertThat(statusOf(after)).isEqualTo(403);
        assertThat(code).isEqualTo(TokenError.INSUFFICIENT_SCOPE.getCode());
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
        for (JsonNode item : settings.path("items")) {
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
        TokenOwner owner = new TokenOwner(ISSUER, subject);
        TokenOptions options = new TokenOptions(scope, expiresAt);
        return tokens.create(owner, slug, "token", options);
    }

    private void expireAt(UUID tokenId, Instant expiresAt) {
        OffsetDateTime value = OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC);
        jdbc().sql("UPDATE api_token SET expires_at = :expiresAt WHERE id = :id")
                .param("expiresAt", value)
                .param("id", tokenId)
                .update();
    }

    private Space seedSpace(String prefix, String subject, SpaceRole role) {
        SpaceService service = bean(SpaceService.class);
        String slug = uniqueSlug(prefix);
        PutResult created = service.put(ADMIN, slug, "Space " + prefix, null);
        Space space = created.space();
        members().addMember(ADMIN, space.slug(), new SpaceMember(ISSUER, subject, role));
        return space;
    }

    private SpaceService spaces() {
        return bean(SpaceService.class);
    }

    private SpaceMembers members() {
        return bean(SpaceMembers.class);
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
        return oidcLogin().idToken(token -> token.subject(subject).issuer(ISSUER));
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
