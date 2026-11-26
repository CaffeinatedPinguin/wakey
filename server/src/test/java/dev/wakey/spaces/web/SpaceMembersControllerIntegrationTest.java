package dev.wakey.spaces.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import dev.wakey.security.CurrentUser;
import dev.wakey.spaces.SpaceMember;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceService;
import dev.wakey.testsupport.SecurityIntegrationTest;
import dev.wakey.web.CommonError;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class SpaceMembersControllerIntegrationTest extends SecurityIntegrationTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private static final String SPACES_PATH = "/api/v1/spaces";

    private static final String ADMIN_ROLE = "ROLE_ADMIN";

    private static final String PROBLEM_JSON = "application/problem+json";

    private static final String ISSUER = "https://issuer.example.com";

    @Test
    void listingMembersIsAPage() throws Exception {
        // given
        String admin = uniqueSubject("member-page");
        String slug = uniqueSlug("member-page");
        seedSpace(slug);
        List<String> subjects = new ArrayList<>();
        for (int index = 0; index < 3; index++) {
            String subject = uniqueSubject("page-member-" + index);
            addMemberToSpace(slug, subject, SpaceRole.VIEWER);
            subjects.add(subject);
        }

        // when
        List<String> listed = new ArrayList<>();
        Optional<String> cursor = Optional.empty();
        int pages = 0;
        do {
            MvcResult result = page(admin, slug, 2, cursor);
            JsonNode body = jsonOf(result);
            assertThat(statusOf(result)).isEqualTo(200);
            listed.addAll(subjectsOf(body));
            cursor = nextCursor(body);
            pages++;
        } while (cursor.isPresent() && pages < 100);

        // then
        assertThat(pages).isGreaterThan(1);
        assertThat(listed).isSorted();
        assertThat(listed).doesNotHaveDuplicates();
        assertThat(listed).containsAll(subjects);
    }

    @Test
    void addingAMemberOverHttpGrantsAccess() throws Exception {
        // given
        String admin = uniqueSubject("member-add");
        String member = uniqueSubject("granted");
        String slug = uniqueSlug("member-add");
        seedSpace(slug);

        // when
        MvcResult added = addMemberAs(admin, true, slug, memberBody(member, SpaceRole.VIEWER));
        MockHttpServletRequestBuilder readRequest =
                get(SPACES_PATH + "/" + slug).with(login(member, false));
        MvcResult read = mockMvc().perform(readRequest).andReturn();

        // then
        JsonNode addedBody = jsonOf(added);
        String addedSubject = addedBody.path("subject").asString();
        String addedRole = addedBody.path("role").asString();
        assertThat(statusOf(added)).isEqualTo(201);
        assertThat(addedSubject).isEqualTo(member);
        assertThat(addedRole).isEqualTo(SpaceRole.VIEWER.name());
        assertThat(statusOf(read)).isEqualTo(200);
    }

    @Test
    void aMemberWithAnInvalidSubjectIsRejectedOverHttp() throws Exception {
        // given
        String admin = uniqueSubject("member-invalid");
        String slug = uniqueSlug("member-invalid");
        seedSpace(slug);
        String tooLong = "x".repeat(SpaceMember.SUBJECT_MAX_LENGTH + 1);

        // when
        MvcResult blank = addMemberAs(admin, true, slug, memberBody("   ", SpaceRole.VIEWER));
        MvcResult longSubject = addMemberAs(admin, true, slug, memberBody(tooLong, SpaceRole.VIEWER));

        // then
        assertValidationProblem(blank, "/subject");
        assertValidationProblem(longSubject, "/subject");
    }

    @Test
    void changingARoleOverHttpInvalidatesTheMembersSessions() throws Exception {
        // given
        String admin = uniqueSubject("role-change-admin");
        String member = uniqueSubject("role-change");
        String slug = uniqueSlug("role-change");
        seedSpace(slug);
        addMemberToSpace(slug, member, SpaceRole.VIEWER);
        Session session = createSession(member);

        // when
        MvcResult changed = changeRoleAs(admin, true, slug, member, SpaceRole.EDITOR);
        MockHttpServletRequestBuilder readRequest =
                get(SPACES_PATH + "/" + slug).with(login(member, false));
        MvcResult read = mockMvc().perform(readRequest).andReturn();

        // then
        JsonNode readBody = jsonOf(read);
        String readRole = readBody.path("role").asString();
        assertThat(statusOf(changed)).isEqualTo(204);
        assertThat(sessionExists(session)).isFalse();
        assertThat(readRole).isEqualTo(SpaceRole.EDITOR.name());
    }

    @Test
    void removingAMemberOverHttpRevokesAccessAndSessions() throws Exception {
        // given
        String admin = uniqueSubject("remove-admin");
        String member = uniqueSubject("removed-member");
        String slug = uniqueSlug("remove-member");
        seedSpace(slug);
        addMemberToSpace(slug, member, SpaceRole.VIEWER);
        Session session = createSession(member);

        // when
        MvcResult removed = removeMemberAs(admin, true, slug, member);
        MockHttpServletRequestBuilder readRequest =
                get(SPACES_PATH + "/" + slug).with(login(member, false));
        MvcResult read = mockMvc().perform(readRequest).andReturn();

        // then
        assertThat(statusOf(removed)).isEqualTo(204);
        assertThat(sessionExists(session)).isFalse();
        assertThat(statusOf(read)).isEqualTo(404);
    }

    @Test
    void aCallerWhoCannotManageTheSpaceCannotChangeMembers() throws Exception {
        // given
        String slug = uniqueSlug("no-manage");
        String viewer = uniqueSubject("viewer-caller");
        String stranger = uniqueSubject("stranger-caller");
        String target = uniqueSubject("managed-target");
        String newMember = uniqueSubject("new-member");
        seedSpace(slug);
        addMemberToSpace(slug, viewer, SpaceRole.VIEWER);
        addMemberToSpace(slug, target, SpaceRole.VIEWER);

        // when
        MvcResult viewerAdd = addMemberAs(viewer, false, slug, memberBody(newMember, SpaceRole.VIEWER));
        MvcResult viewerChange = changeRoleAs(viewer, false, slug, target, SpaceRole.EDITOR);
        MvcResult viewerRemove = removeMemberAs(viewer, false, slug, target);
        MvcResult strangerAdd = addMemberAs(stranger, false, slug, memberBody(newMember, SpaceRole.VIEWER));
        MvcResult strangerChange = changeRoleAs(stranger, false, slug, target, SpaceRole.EDITOR);
        MvcResult strangerRemove = removeMemberAs(stranger, false, slug, target);

        // then
        assertThat(statusOf(viewerAdd)).isEqualTo(403);
        assertThat(statusOf(viewerChange)).isEqualTo(403);
        assertThat(statusOf(viewerRemove)).isEqualTo(403);
        assertThat(statusOf(strangerAdd)).isEqualTo(404);
        assertThat(statusOf(strangerChange)).isEqualTo(404);
        assertThat(statusOf(strangerRemove)).isEqualTo(404);
    }

    @Test
    void changingTheRoleOfAnUnknownMemberIsNotFound() throws Exception {
        // given
        String admin = uniqueSubject("unknown-role");
        String slug = uniqueSlug("unknown-role");
        String unknown = uniqueSubject("unknown");
        seedSpace(slug);

        // when
        MvcResult result = changeRoleAs(admin, true, slug, unknown, SpaceRole.EDITOR);

        // then
        JsonNode body = jsonOf(result);
        String code = body.path("code").asString();
        assertThat(statusOf(result)).isEqualTo(404);
        assertThat(contentTypeOf(result)).isEqualTo(PROBLEM_JSON);
        assertThat(code).isEqualTo(CommonError.NOT_FOUND.getCode());
    }

    private MvcResult page(String admin, String slug, int limit, Optional<String> cursor) throws Exception {
        MockHttpServletRequestBuilder request = get(SPACES_PATH + "/" + slug + "/members")
                .param("limit", Integer.toString(limit))
                .with(login(admin, true));
        if (cursor.isPresent()) {
            request = request.param("cursor", cursor.get());
        }
        return mockMvc().perform(request).andReturn();
    }

    private MvcResult addMemberAs(String actor, boolean admin, String slug, String body) throws Exception {
        MockHttpServletRequestBuilder request = post(SPACES_PATH + "/" + slug + "/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(login(actor, admin))
                .with(csrf());
        return mockMvc().perform(request).andReturn();
    }

    private MvcResult changeRoleAs(String actor, boolean admin, String slug, String subject, SpaceRole role)
            throws Exception {
        MockHttpServletRequestBuilder request = put(SPACES_PATH + "/" + slug + "/members/role")
                .param("issuer", ISSUER)
                .param("subject", subject)
                .contentType(MediaType.APPLICATION_JSON)
                .content(roleBody(role))
                .with(login(actor, admin))
                .with(csrf());
        return mockMvc().perform(request).andReturn();
    }

    private MvcResult removeMemberAs(String actor, boolean admin, String slug, String subject) throws Exception {
        MockHttpServletRequestBuilder request = delete(SPACES_PATH + "/" + slug + "/members")
                .param("issuer", ISSUER)
                .param("subject", subject)
                .with(login(actor, admin))
                .with(csrf());
        return mockMvc().perform(request).andReturn();
    }

    private void seedSpace(String slug) {
        SpaceService service = bean(SpaceService.class);
        service.put(fixtureAdmin(), slug, "Members space");
    }

    private void addMemberToSpace(String slug, String subject, SpaceRole role) {
        SpaceService service = bean(SpaceService.class);
        service.addMember(fixtureAdmin(), slug, new SpaceMember(ISSUER, subject, role));
    }

    private Session createSession(String principal) {
        JdbcIndexedSessionRepository repository = bean(JdbcIndexedSessionRepository.class);
        return createSession(repository, principal);
    }

    private static <S extends Session> S createSession(SessionRepository<S> repository, String principal) {
        S session = repository.createSession();
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, "n/a", authorities);
        SecurityContextImpl context = new SecurityContextImpl(authentication);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, principal);
        repository.save(session);
        return session;
    }

    private boolean sessionExists(Session session) {
        JdbcIndexedSessionRepository repository = bean(JdbcIndexedSessionRepository.class);
        return repository.findById(session.getId()) != null;
    }

    private static void assertValidationProblem(MvcResult result, String pointer) throws Exception {
        JsonNode body = jsonOf(result);
        String code = body.path("code").asString();
        assertThat(statusOf(result)).isEqualTo(400);
        assertThat(contentTypeOf(result)).isEqualTo(PROBLEM_JSON);
        assertThat(code).isEqualTo(CommonError.VALIDATION_FAILED.getCode());
        assertThat(errorPointers(body)).contains(pointer);
    }

    private static List<String> errorPointers(JsonNode body) {
        List<String> pointers = new ArrayList<>();
        for (JsonNode error : body.path("errors")) {
            String pointer = error.path("pointer").asString();
            pointers.add(pointer);
        }
        return pointers;
    }

    private static String memberBody(String subject, SpaceRole role) {
        return "{\"issuer\":\"" + ISSUER + "\",\"subject\":\"" + subject + "\",\"role\":\"" + role.name() + "\"}";
    }

    private static String roleBody(SpaceRole role) {
        return "{\"role\":\"" + role.name() + "\"}";
    }

    private static List<String> subjectsOf(JsonNode page) {
        List<String> subjects = new ArrayList<>();
        for (JsonNode item : page.path("items")) {
            String subject = item.path("subject").asString();
            subjects.add(subject);
        }
        return subjects;
    }

    private static Optional<String> nextCursor(JsonNode body) {
        JsonNode nextCursor = body.path("nextCursor");
        if (nextCursor.isNull() || nextCursor.isMissingNode()) {
            return Optional.empty();
        }
        return Optional.of(nextCursor.asString());
    }

    private static RequestPostProcessor login(String subject, boolean admin) {
        if (admin) {
            return oidcLogin()
                    .idToken(token -> token.subject(subject).issuer(ISSUER))
                    .authorities(new SimpleGrantedAuthority(ADMIN_ROLE));
        }
        return oidcLogin().idToken(token -> token.subject(subject).issuer(ISSUER));
    }

    private static CurrentUser fixtureAdmin() {
        return new CurrentUser(ISSUER, "fixture-admin", true);
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
