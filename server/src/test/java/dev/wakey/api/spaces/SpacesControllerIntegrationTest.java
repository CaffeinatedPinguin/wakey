package dev.wakey.api.spaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import dev.wakey.spaces.PutResult;
import dev.wakey.spaces.Space;
import dev.wakey.spaces.SpaceError;
import dev.wakey.spaces.SpacePrincipal;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceService;
import dev.wakey.testsupport.SecurityIntegrationTest;
import dev.wakey.web.CommonError;
import dev.wakey.web.CursorCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class SpacesControllerIntegrationTest extends SecurityIntegrationTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private static final String SPACES_PATH = "/api/v1/spaces";

    private static final String ADMIN_ROLE = "ROLE_ADMIN";

    private static final String PROBLEM_JSON = "application/problem+json";

    @Test
    void adminPutsASpaceAndRepeatingItIsIdempotent() throws Exception {
        // given
        String subject = uniqueSubject("admin-create");
        String slug = uniqueSlug("idempotent");
        String body = nameBody("Created space");

        // when
        MvcResult first = putSpace(subject, true, slug, body, true);
        MvcResult second = putSpace(subject, true, slug, body, true);

        // then
        int firstStatus = statusOf(first);
        int secondStatus = statusOf(second);
        JsonNode firstBody = jsonOf(first);
        JsonNode secondBody = jsonOf(second);
        String firstVersion = firstBody.path("version").asString();
        String secondVersion = secondBody.path("version").asString();
        String secondSlug = secondBody.path("slug").asString();
        String secondName = secondBody.path("name").asString();
        assertThat(firstStatus).isEqualTo(201);
        assertThat(secondStatus).isEqualTo(200);
        assertThat(secondVersion).isEqualTo(firstVersion);
        assertThat(secondSlug).isEqualTo(slug);
        assertThat(secondName).isEqualTo("Created space");
    }

    @Test
    void renamingViaPutBumpsTheVersion() throws Exception {
        // given
        String subject = uniqueSubject("admin-rename");
        String slug = uniqueSlug("rename");

        // when
        MvcResult created = putSpace(subject, true, slug, nameBody("Old name"), true);
        MvcResult renamed = putSpace(subject, true, slug, nameBody("New name"), true);

        // then
        JsonNode createdBody = jsonOf(created);
        JsonNode renamedBody = jsonOf(renamed);
        long createdVersion = createdBody.path("version").asLong();
        long renamedVersion = renamedBody.path("version").asLong();
        String renamedName = renamedBody.path("name").asString();
        int renamedStatus = statusOf(renamed);
        assertThat(renamedStatus).isEqualTo(200);
        assertThat(renamedVersion).isEqualTo(createdVersion + 1);
        assertThat(renamedName).isEqualTo("New name");
    }

    @Test
    void nonAdminPutIsForbiddenAsProblem() throws Exception {
        // given
        String subject = uniqueSubject("non-admin");
        String slug = uniqueSlug("non-admin-write");

        // when
        MvcResult result = putSpace(subject, false, slug, nameBody("Forbidden"), true);

        // then
        JsonNode body = jsonOf(result);
        String code = body.path("code").asString();
        assertThat(statusOf(result)).isEqualTo(403);
        assertThat(contentTypeOf(result)).isEqualTo(PROBLEM_JSON);
        assertThat(code).isEqualTo(CommonError.FORBIDDEN.getCode());
    }

    @Test
    void strangerGetsProblemNotFoundForAnotherSpace() throws Exception {
        // given
        String owner = uniqueSubject("owner");
        String stranger = uniqueSubject("stranger");
        String slug = uniqueSlug("stranger");
        seedSpace(slug, "Stranger space", owner, SpaceRole.EDITOR);

        // when
        MockHttpServletRequestBuilder request = get(SPACES_PATH + "/" + slug).with(login(stranger, false));
        MvcResult result = mockMvc().perform(request).andReturn();

        // then
        JsonNode body = jsonOf(result);
        String code = body.path("code").asString();
        assertThat(statusOf(result)).isEqualTo(404);
        assertThat(contentTypeOf(result)).isEqualTo(PROBLEM_JSON);
        assertThat(code).isEqualTo(CommonError.NOT_FOUND.getCode());
    }

    @Test
    void viewerReadsButCannotPut() throws Exception {
        // given
        String viewer = uniqueSubject("viewer");
        String slug = uniqueSlug("viewer");
        seedSpace(slug, "Viewer space", viewer, SpaceRole.VIEWER);

        // when
        MockHttpServletRequestBuilder readRequest =
                get(SPACES_PATH + "/" + slug).with(login(viewer, false));
        MvcResult read = mockMvc().perform(readRequest).andReturn();
        MvcResult write = putSpace(viewer, false, slug, nameBody("Viewer rename"), true);

        // then
        JsonNode readBody = jsonOf(read);
        String readSlug = readBody.path("slug").asString();
        String role = readBody.path("role").asString();
        assertThat(statusOf(read)).isEqualTo(200);
        assertThat(readSlug).isEqualTo(slug);
        assertThat(role).isEqualTo(SpaceRole.VIEWER.name());
        assertThat(statusOf(write)).isEqualTo(403);
    }

    @Test
    void gettingOneSpaceDoesNotListAllSpaces() throws Exception {
        // given
        String member = uniqueSubject("single-read");
        String targetSlug = uniqueSlug("target");
        seedSpace(targetSlug, "Target space", member, SpaceRole.VIEWER);
        seedSpace(uniqueSlug("other"), "Other space", uniqueSubject("other"), SpaceRole.EDITOR);
        seedSpace(uniqueSlug("third"), "Third space", uniqueSubject("third"), SpaceRole.EDITOR);

        // when
        MockHttpServletRequestBuilder request =
                get(SPACES_PATH + "/" + targetSlug).with(login(member, false));
        MvcResult result = mockMvc().perform(request).andReturn();

        // then
        JsonNode body = jsonOf(result);
        String slug = body.path("slug").asString();
        String name = body.path("name").asString();
        String role = body.path("role").asString();
        assertThat(statusOf(result)).isEqualTo(200);
        assertThat(slug).isEqualTo(targetSlug);
        assertThat(name).isEqualTo("Target space");
        assertThat(role).isEqualTo(SpaceRole.VIEWER.name());
    }

    @Test
    void memberSeesOnlyOwnSpacesInTheList() throws Exception {
        // given
        String member = uniqueSubject("member-list");
        String ownSlug = uniqueSlug("own");
        String foreignSlug = uniqueSlug("foreign");
        seedSpace(ownSlug, "Own space", member, SpaceRole.VIEWER);
        seedSpace(foreignSlug, "Foreign space", uniqueSubject("other"), SpaceRole.EDITOR);

        // when
        MockHttpServletRequestBuilder request = get(SPACES_PATH).with(login(member, false));
        MvcResult result = mockMvc().perform(request).andReturn();

        // then
        JsonNode body = jsonOf(result);
        List<String> slugs = slugsOf(body);
        assertThat(statusOf(result)).isEqualTo(200);
        assertThat(slugs).contains(ownSlug).doesNotContain(foreignSlug);
    }

    @Test
    void listIsPagedByCursorAndSortedBySlug() throws Exception {
        // given
        String admin = uniqueSubject("admin-page");
        String prefix = uniqueSlug("page");
        List<String> created = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            String slug = prefix + "-" + index;
            seedSpace(slug, "Page " + index, uniqueSubject("member"), SpaceRole.VIEWER);
            created.add(slug);
        }

        // when
        List<String> allSlugs = new ArrayList<>();
        Optional<String> cursor = Optional.empty();
        int pages = 0;
        do {
            MvcResult result = page(admin, 2, cursor);
            JsonNode body = jsonOf(result);
            allSlugs.addAll(slugsOf(body));
            cursor = nextCursor(body);
            pages++;
        } while (cursor.isPresent() && pages < 100);

        // then
        assertThat(pages).isGreaterThan(1);
        assertThat(allSlugs).isSorted();
        assertThat(allSlugs).doesNotHaveDuplicates();
        assertThat(allSlugs).containsAll(created);
    }

    @Test
    void aCursorWithAnInvalidSlugIsRejected() throws Exception {
        // given
        String admin = uniqueSubject("admin-bad-cursor");
        String cursor = CursorCodec.encode("Not A Slug");

        // when
        MvcResult result = page(admin, 2, Optional.of(cursor));

        // then
        JsonNode body = jsonOf(result);
        String code = body.path("code").asString();
        assertThat(statusOf(result)).isEqualTo(400);
        assertThat(contentTypeOf(result)).isEqualTo(PROBLEM_JSON);
        assertThat(code).isEqualTo(CommonError.INVALID_CURSOR.getCode());
    }

    @Test
    void invalidSlugIsAValidationProblemWithPointer() throws Exception {
        // given
        String admin = uniqueSubject("admin-invalid-slug");

        // when
        MvcResult result = putSpace(admin, true, "ab", nameBody("Invalid slug"), true);

        // then
        JsonNode body = jsonOf(result);
        JsonNode errors = body.path("errors");
        JsonNode firstError = errors.path(0);
        String code = body.path("code").asString();
        String pointer = firstError.path("pointer").asString();
        String fieldCode = firstError.path("code").asString();
        assertThat(statusOf(result)).isEqualTo(400);
        assertThat(contentTypeOf(result)).isEqualTo(PROBLEM_JSON);
        assertThat(code).isEqualTo(CommonError.VALIDATION_FAILED.getCode());
        assertThat(pointer).isEqualTo("/slug");
        assertThat(fieldCode).isEqualTo(SpaceError.INVALID_SLUG.getCode());
    }

    @Test
    void blankNameIsAValidationProblemWithPointer() throws Exception {
        // given
        String admin = uniqueSubject("admin-blank");
        String slug = uniqueSlug("blank-name");

        // when
        MvcResult result = putSpace(admin, true, slug, nameBody("   "), true);

        // then
        JsonNode body = jsonOf(result);
        JsonNode firstError = body.path("errors").path(0);
        String code = body.path("code").asString();
        String pointer = firstError.path("pointer").asString();
        assertThat(statusOf(result)).isEqualTo(400);
        assertThat(code).isEqualTo(CommonError.VALIDATION_FAILED.getCode());
        assertThat(pointer).isEqualTo("/name");
    }

    @Test
    void putWithoutCsrfTokenIsForbidden() throws Exception {
        // given
        String admin = uniqueSubject("admin-csrf");
        String slug = uniqueSlug("no-csrf");

        // when
        MvcResult result = putSpace(admin, true, slug, nameBody("No csrf"), false);

        // then
        JsonNode body = jsonOf(result);
        String code = body.path("code").asString();
        assertThat(statusOf(result)).isEqualTo(403);
        assertThat(contentTypeOf(result)).isEqualTo(PROBLEM_JSON);
        assertThat(code).isEqualTo(CommonError.FORBIDDEN.getCode());
    }

    @Test
    void springMappedClientErrorsStayClientErrors() throws Exception {
        // given
        String admin = uniqueSubject("mapped-errors");
        MockHttpServletRequestBuilder badLimitRequest =
                get(SPACES_PATH).param("limit", "abc").with(login(admin, true));
        MockHttpServletRequestBuilder wrongMethodRequest =
                post(SPACES_PATH).with(login(admin, true)).with(csrf());

        // when
        MvcResult badLimit = mockMvc().perform(badLimitRequest).andReturn();
        MvcResult wrongMethod = mockMvc().perform(wrongMethodRequest).andReturn();

        // then
        JsonNode badLimitBody = jsonOf(badLimit);
        JsonNode wrongMethodBody = jsonOf(wrongMethod);
        String badLimitCode = badLimitBody.path("code").asString();
        String wrongMethodCode = wrongMethodBody.path("code").asString();
        assertThat(statusOf(badLimit)).isEqualTo(400);
        assertThat(contentTypeOf(badLimit)).isEqualTo(PROBLEM_JSON);
        assertThat(badLimitCode).isEqualTo(CommonError.BAD_REQUEST.getCode());
        assertThat(statusOf(wrongMethod)).isEqualTo(405);
        assertThat(contentTypeOf(wrongMethod)).isEqualTo(PROBLEM_JSON);
        assertThat(wrongMethodCode).isEqualTo(CommonError.METHOD_NOT_ALLOWED.getCode());
    }

    @Test
    void unsupportedMethodKeepsTheAllowHeader() throws Exception {
        // given
        String admin = uniqueSubject("allow-header");
        MockHttpServletRequestBuilder request =
                post(SPACES_PATH).with(login(admin, true)).with(csrf());

        // when
        MvcResult result = mockMvc().perform(request).andReturn();

        // then
        JsonNode body = jsonOf(result);
        String allow = result.getResponse().getHeader(HttpHeaders.ALLOW);
        String code = body.path("code").asString();
        assertThat(statusOf(result)).isEqualTo(405);
        assertThat(allow).contains("GET");
        assertThat(code).isEqualTo(CommonError.METHOD_NOT_ALLOWED.getCode());
    }

    @Test
    void putWithIfNoneMatchStarRefusesAnExistingSlug() throws Exception {
        // given
        String admin = uniqueSubject("if-none-match");
        String slug = uniqueSlug("existing");
        putSpace(admin, true, slug, nameBody("Original name"), true);

        // when
        MockHttpServletRequestBuilder request = put(SPACES_PATH + "/" + slug)
                .contentType(MediaType.APPLICATION_JSON)
                .content(nameBody("Renamed"))
                .header("If-None-Match", "*")
                .with(login(admin, true))
                .with(csrf());
        MvcResult refused = mockMvc().perform(request).andReturn();
        MockHttpServletRequestBuilder readRequest =
                get(SPACES_PATH + "/" + slug).with(login(admin, true));
        MvcResult read = mockMvc().perform(readRequest).andReturn();

        // then
        JsonNode refusedBody = jsonOf(refused);
        JsonNode readBody = jsonOf(read);
        String refusedCode = refusedBody.path("code").asString();
        String readName = readBody.path("name").asString();
        assertThat(statusOf(refused)).isEqualTo(409);
        assertThat(contentTypeOf(refused)).isEqualTo(PROBLEM_JSON);
        assertThat(refusedCode).isEqualTo(SpaceError.SLUG_TAKEN.getCode());
        assertThat(readName).isEqualTo("Original name");
    }

    @Test
    void putWithoutTheHeaderStillRenames() throws Exception {
        // given
        String admin = uniqueSubject("no-if-none-match");
        String slug = uniqueSlug("rename-without-header");
        putSpace(admin, true, slug, nameBody("Old name"), true);

        // when
        MvcResult renamed = putSpace(admin, true, slug, nameBody("New name"), true);

        // then
        JsonNode body = jsonOf(renamed);
        String renamedName = body.path("name").asString();
        assertThat(statusOf(renamed)).isEqualTo(200);
        assertThat(renamedName).isEqualTo("New name");
    }

    @Test
    void renamingWithAStaleIfMatchIsAConflict() throws Exception {
        // given
        String admin = uniqueSubject("stale-if-match");
        String slug = uniqueSlug("stale-if-match");
        MvcResult created = putSpace(admin, true, slug, nameBody("Original name"), true);
        long createdVersion = jsonOf(created).path("version").asLong();

        // when
        MvcResult stale = putWithHeader(admin, slug, nameBody("Stale name"), "If-Match", quoted(createdVersion - 1));
        MockHttpServletRequestBuilder readRequest =
                get(SPACES_PATH + "/" + slug).with(login(admin, true));
        MvcResult read = mockMvc().perform(readRequest).andReturn();

        // then
        JsonNode staleBody = jsonOf(stale);
        String staleCode = staleBody.path("code").asString();
        String storedName = jsonOf(read).path("name").asString();
        assertThat(statusOf(stale)).isEqualTo(409);
        assertThat(staleCode).isEqualTo(SpaceError.VERSION_CONFLICT.getCode());
        assertThat(storedName).isEqualTo("Original name");
    }

    @Test
    void renamingWithTheCurrentVersionSucceeds() throws Exception {
        // given
        String admin = uniqueSubject("current-if-match");
        String slug = uniqueSlug("current-if-match");
        MvcResult created = putSpace(admin, true, slug, nameBody("Original name"), true);
        long createdVersion = jsonOf(created).path("version").asLong();

        // when
        MvcResult renamed = putWithHeader(admin, slug, nameBody("Renamed name"), "If-Match", quoted(createdVersion));

        // then
        JsonNode body = jsonOf(renamed);
        String renamedName = body.path("name").asString();
        long renamedVersion = body.path("version").asLong();
        assertThat(statusOf(renamed)).isEqualTo(200);
        assertThat(renamedName).isEqualTo("Renamed name");
        assertThat(renamedVersion).isEqualTo(createdVersion + 1);
    }

    @Test
    void anIfNoneMatchOtherThanStarIsRejected() throws Exception {
        // given
        String admin = uniqueSubject("bad-if-none-match");
        String slug = uniqueSlug("bad-if-none-match");

        // when
        MvcResult rejected = putWithHeader(admin, slug, nameBody("Rejected"), "If-None-Match", "\"1\"");

        // then
        JsonNode body = jsonOf(rejected);
        String code = body.path("code").asString();
        assertThat(statusOf(rejected)).isEqualTo(400);
        assertThat(code).isEqualTo(SpaceError.INVALID_PRECONDITION.getCode());
    }

    @Test
    void aNameWithControlOrBidiCharactersIsRejected() throws Exception {
        // given
        String admin = uniqueSubject("bidi-name");
        String slug = uniqueSlug("bidi-name");

        // when
        MvcResult rejected = putSpace(admin, true, slug, "{\"name\":\"Bad\\u202ename\"}", true);

        // then
        JsonNode body = jsonOf(rejected);
        JsonNode firstError = body.path("errors").path(0);
        String code = body.path("code").asString();
        String pointer = firstError.path("pointer").asString();
        String fieldCode = firstError.path("code").asString();
        assertThat(statusOf(rejected)).isEqualTo(400);
        assertThat(code).isEqualTo(CommonError.VALIDATION_FAILED.getCode());
        assertThat(pointer).isEqualTo("/name");
        assertThat(fieldCode).isEqualTo(SpaceError.INVALID_NAME.getCode());
    }

    @Test
    void aNameWithANulCharacterIsAValidationErrorNot500() throws Exception {
        // given
        String admin = uniqueSubject("nul-name");
        String slug = uniqueSlug("nul-name");

        // when
        MvcResult rejected = putSpace(admin, true, slug, "{\"name\":\"Nul\\u0000name\"}", true);

        // then
        JsonNode body = jsonOf(rejected);
        JsonNode firstError = body.path("errors").path(0);
        String code = body.path("code").asString();
        String fieldCode = firstError.path("code").asString();
        assertThat(statusOf(rejected)).isEqualTo(400);
        assertThat(code).isEqualTo(CommonError.VALIDATION_FAILED.getCode());
        assertThat(fieldCode).isEqualTo(SpaceError.INVALID_NAME.getCode());
    }

    @Test
    void aCreatedSpaceHasALocationHeader() throws Exception {
        // given
        String admin = uniqueSubject("location-admin");
        String slug = uniqueSlug("location");

        // when
        MvcResult created = putSpace(admin, true, slug, nameBody("Located space"), true);

        // then
        String location = created.getResponse().getHeader("Location");
        assertThat(statusOf(created)).isEqualTo(201);
        assertThat(location).isEqualTo(SPACES_PATH + "/" + slug);
    }

    @Test
    void spaceResponseHasNoAdminField() throws Exception {
        // given
        String admin = uniqueSubject("no-admin-field");
        String slug = uniqueSlug("no-admin-field");
        putSpace(admin, true, slug, nameBody("Visible space"), true);

        // when
        MockHttpServletRequestBuilder readRequest =
                get(SPACES_PATH + "/" + slug).with(login(admin, true));
        MvcResult read = mockMvc().perform(readRequest).andReturn();

        // then
        JsonNode body = jsonOf(read);
        assertThat(statusOf(read)).isEqualTo(200);
        assertThat(body.has("admin")).isFalse();
        String responseSlug = body.path("slug").asString();
        assertThat(responseSlug).isEqualTo(slug);
    }

    private MvcResult putWithHeader(String subject, String slug, String body, String header, String value)
            throws Exception {
        var request = put(SPACES_PATH + "/" + slug)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header(header, value)
                .with(login(subject, true))
                .with(csrf());
        return mockMvc().perform(request).andReturn();
    }

    private MvcResult putSpace(String subject, boolean admin, String slug, String body, boolean withCsrf)
            throws Exception {
        var request = put(SPACES_PATH + "/" + slug)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(login(subject, admin));
        if (withCsrf) {
            request = request.with(csrf());
        }
        return mockMvc().perform(request).andReturn();
    }

    private MvcResult page(String subject, int limit, Optional<String> cursor) throws Exception {
        MockHttpServletRequestBuilder request =
                get(SPACES_PATH).param("limit", Integer.toString(limit)).with(login(subject, true));
        if (cursor.isPresent()) {
            request = request.param("cursor", cursor.get());
        }
        return mockMvc().perform(request).andReturn();
    }

    private Space seedSpace(String slug, String name, String member, SpaceRole role) {
        SpaceService service = bean(SpaceService.class);
        PutResult created = service.put(fixtureAdmin(), slug, name);
        service.addMember(fixtureAdmin(), slug, member, role);
        return created.space();
    }

    private static RequestPostProcessor login(String subject, boolean admin) {
        if (admin) {
            return oidcLogin()
                    .idToken(token -> token.subject(subject))
                    .authorities(new SimpleGrantedAuthority(ADMIN_ROLE));
        }
        return oidcLogin().idToken(token -> token.subject(subject));
    }

    private static SpacePrincipal fixtureAdmin() {
        return new SpacePrincipal("fixture-admin", true);
    }

    private static String nameBody(String name) {
        return "{\"name\":\"" + name + "\"}";
    }

    private static String quoted(long version) {
        return "\"" + version + "\"";
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

    private static List<String> slugsOf(JsonNode page) {
        List<String> slugs = new ArrayList<>();
        for (JsonNode item : page.path("items")) {
            String slug = item.path("slug").asString();
            slugs.add(slug);
        }
        return slugs;
    }

    private static Optional<String> nextCursor(JsonNode body) {
        JsonNode nextCursor = body.path("nextCursor");
        if (nextCursor.isNull() || nextCursor.isMissingNode()) {
            return Optional.empty();
        }
        return Optional.of(nextCursor.asString());
    }

    private static String uniqueSlug(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String uniqueSubject(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
