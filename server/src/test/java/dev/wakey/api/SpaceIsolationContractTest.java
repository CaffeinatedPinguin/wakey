package dev.wakey.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import dev.wakey.spaces.SpaceMember;
import dev.wakey.spaces.SpaceMembers;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceService;
import dev.wakey.testsupport.ContractFile;
import dev.wakey.testsupport.SecurityIntegrationTest;
import dev.wakey.tokens.ApiTokens;
import dev.wakey.tokens.CreatedToken;
import dev.wakey.tokens.TokenOptions;
import dev.wakey.tokens.TokenOwner;
import dev.wakey.tokens.TokenScope;
import dev.wakey.web.CurrentUser;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * The contract-driven isolation test: every operation with a {@code {slug}} is asked the same four
 * questions, so a new operation is covered without editing this test.
 */
class SpaceIsolationContractTest extends SecurityIntegrationTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private static final String ISSUER = "https://issuer.example.com";

    private static final CurrentUser ADMIN = new CurrentUser(ISSUER, "fixture-admin", true);

    private static final String SLUG = "{slug}";

    private static final String ID = "{id}";

    private static final String KEY = "{key}";

    private static final String SAMPLE_KEY = "timezone";

    private static final String EMPTY_BODY = "{}";

    private static final List<String> HTTP_METHODS = List.of("get", "post", "put", "patch", "delete");

    private static final Set<String> WRITE_METHODS = Set.of("post", "put", "patch", "delete");

    @Test
    void everySpaceOperationRejectsAnUnauthenticatedCaller() throws Exception {
        // when
        List<SpaceOperation> operations = spaceOperations();

        // then
        for (SpaceOperation operation : operations) {
            MockHttpServletRequestBuilder request = buildRequest(operation, uniqueSlug("unauth"));
            MvcResult result = mockMvc().perform(request).andReturn();
            assertThat(statusOf(result)).as(operation.toString()).isEqualTo(401);
        }
    }

    @Test
    void aTokenOfAnotherSpaceSeesNotFoundOnEverySpaceOperation() throws Exception {
        // given
        String owner = uniqueSubject("token-owner");
        String ownSlug = uniqueSlug("token-own");
        String targetSlug = uniqueSlug("token-target");
        seedSpace(ownSlug, owner, SpaceRole.EDITOR);
        seedSpace(targetSlug, uniqueSubject("target-owner"), SpaceRole.EDITOR);
        CreatedToken readToken = createToken(owner, ownSlug, TokenScope.READ);
        CreatedToken writeToken = createToken(owner, ownSlug, TokenScope.WRITE);
        List<SpaceOperation> operations = spaceOperations();

        // when
        for (SpaceOperation operation : operations) {
            CreatedToken token = operation.write() ? writeToken : readToken;
            MockHttpServletRequestBuilder request = buildRequest(operation, targetSlug);
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.secret());
            MvcResult result = mockMvc().perform(request).andReturn();

            // then
            assertThat(statusOf(result)).as(operation.toString()).isEqualTo(404);
        }
    }

    @Test
    void aStrangerSeesNotFoundOnEverySpaceOperation() throws Exception {
        // given
        String stranger = uniqueSubject("stranger");
        String targetSlug = uniqueSlug("stranger-target");
        seedSpace(targetSlug, uniqueSubject("target-owner"), SpaceRole.EDITOR);
        RequestPostProcessor login = login(stranger);
        List<SpaceOperation> operations = spaceOperations();

        // when
        for (SpaceOperation operation : operations) {
            MockHttpServletRequestBuilder request = buildRequest(operation, targetSlug);
            request = request.with(login);
            MvcResult result = mockMvc().perform(request).andReturn();

            // then
            assertThat(statusOf(result)).as(operation.toString()).isEqualTo(404);
        }
    }

    @Test
    void aViewerCannotWriteOnAnyWriteOperation() throws Exception {
        // given
        String viewer = uniqueSubject("viewer");
        String targetSlug = uniqueSlug("viewer-target");
        seedSpace(targetSlug, uniqueSubject("target-owner"), SpaceRole.EDITOR);
        addMember(targetSlug, viewer, SpaceRole.VIEWER);
        RequestPostProcessor login = login(viewer);
        List<SpaceOperation> operations = writeOperations();

        // when
        for (SpaceOperation operation : operations) {
            MockHttpServletRequestBuilder request = buildRequest(operation, targetSlug);
            request = request.with(login);
            MvcResult result = mockMvc().perform(request).andReturn();

            // then
            assertThat(statusOf(result)).as(operation.toString()).isEqualTo(403);
        }
        assertThat(operations).isNotEmpty();
    }

    private MockHttpServletRequestBuilder buildRequest(SpaceOperation operation, String slug) {
        String path = concretePath(operation.path(), slug);
        String methodName = operation.method().toUpperCase(Locale.ROOT);
        HttpMethod method = HttpMethod.valueOf(methodName);
        MockHttpServletRequestBuilder builder = request(method, path);
        if (operation.write()) {
            builder = builder.contentType(MediaType.APPLICATION_JSON).content(EMPTY_BODY);
        }
        return builder.with(csrf());
    }

    private static String concretePath(String template, String slug) {
        String withSlug = template.replace(SLUG, slug);
        String id = UUID.randomUUID().toString();
        String withId = withSlug.replace(ID, id);
        return withId.replace(KEY, SAMPLE_KEY);
    }

    private static List<SpaceOperation> spaceOperations() throws Exception {
        String contract = Files.readString(ContractFile.path());
        JsonNode paths = MAPPER.readTree(contract).path("paths");
        List<SpaceOperation> operations = new ArrayList<>();
        for (String path : paths.propertyNames()) {
            addOperations(path, paths.path(path), operations);
        }
        return operations;
    }

    private static void addOperations(String path, JsonNode methods, List<SpaceOperation> operations) {
        if (path.contains(SLUG)) {
            for (String method : HTTP_METHODS) {
                JsonNode operation = methods.path(method);
                if (operation.isObject()) {
                    operations.add(new SpaceOperation(method, path));
                }
            }
        }
    }

    private static List<SpaceOperation> writeOperations() throws Exception {
        List<SpaceOperation> writes = new ArrayList<>();
        for (SpaceOperation operation : spaceOperations()) {
            if (operation.write()) {
                writes.add(operation);
            }
        }
        return writes;
    }

    private void seedSpace(String slug, String member, SpaceRole role) {
        SpaceService spaces = bean(SpaceService.class);
        spaces.put(ADMIN, slug, "Space " + slug, null);
        addMember(slug, member, role);
    }

    private void addMember(String slug, String subject, SpaceRole role) {
        SpaceMembers members = bean(SpaceMembers.class);
        members.addMember(ADMIN, slug, new SpaceMember(ISSUER, subject, role));
    }

    private CreatedToken createToken(String subject, String slug, TokenScope scope) {
        ApiTokens tokens = bean(ApiTokens.class);
        TokenOwner owner = new TokenOwner(ISSUER, subject);
        TokenOptions options = new TokenOptions(scope, null);
        return tokens.create(owner, slug, "contract", options);
    }

    private static RequestPostProcessor login(String subject) {
        return oidcLogin().idToken(token -> token.subject(subject).issuer(ISSUER));
    }

    private static int statusOf(MvcResult result) {
        return result.getResponse().getStatus();
    }

    private static String uniqueSlug(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String uniqueSubject(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    record SpaceOperation(String method, String path) {

        boolean write() {
            return WRITE_METHODS.contains(method);
        }
    }
}
