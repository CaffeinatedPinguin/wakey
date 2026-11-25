package dev.wakey.spaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import dev.wakey.testsupport.PostgresIntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;

class SpaceServiceIntegrationTest extends PostgresIntegrationTest {

    private static final SpacePrincipal ADMIN = new SpacePrincipal("fixture-admin", true);

    private static final SpacePrincipal EDITOR = new SpacePrincipal("fixture-editor", false);

    private static final SpacePrincipal VIEWER = new SpacePrincipal("fixture-viewer", false);

    private static final SpacePrincipal STRANGER = new SpacePrincipal("fixture-stranger", false);

    @Test
    void adminCreatesASpaceAndPutIsIdempotent() {
        // given
        SpaceService service = service();
        String slug = uniqueSlug("create");
        String name = "Created space";

        // when
        PutResult first = service.put(ADMIN, slug, name);
        PutResult second = service.put(ADMIN, slug, "  " + name + "  ");

        // then
        Space firstSpace = first.space();
        Space secondSpace = second.space();
        assertThat(first.created()).isTrue();
        assertThat(firstSpace.slug()).isEqualTo(slug);
        assertThat(second.created()).isFalse();
        assertThat(secondSpace.id()).isEqualTo(firstSpace.id());
        assertThat(secondSpace.version()).isEqualTo(firstSpace.version());
        assertThat(secondSpace.name()).isEqualTo(name);
    }

    @Test
    void renamingASpaceBumpsTheVersion() {
        // given
        SpaceService service = service();
        String slug = uniqueSlug("rename");
        PutResult created = service.put(ADMIN, slug, "Old name");

        // when
        PutResult renamed = service.put(ADMIN, slug, "New name");
        SpaceView stored = service.view(ADMIN, slug);

        // then
        Space renamedSpace = renamed.space();
        Space createdSpace = created.space();
        assertThat(renamed.created()).isFalse();
        assertThat(renamedSpace.version()).isEqualTo(createdSpace.version() + 1);
        assertThat(renamedSpace.name()).isEqualTo("New name");
        assertThat(stored.name()).isEqualTo("New name");
    }

    @Test
    void nonAdminCannotCreateASpace() {
        // given
        SpaceService service = service();
        Space existing = seedSpace("denied");
        String newSlug = uniqueSlug("denied-new");

        // when
        Throwable creation = catchThrowable(() -> service.put(EDITOR, newSlug, "Denied"));
        Throwable rename = catchThrowable(() -> service.put(EDITOR, existing.slug(), "Renamed by editor"));
        Throwable viewerCreation = catchThrowable(() -> service.put(VIEWER, newSlug, "Denied"));

        // then
        assertThat(creation).isInstanceOf(SpaceAccessDeniedException.class);
        assertThat(rename).isInstanceOf(SpaceAccessDeniedException.class);
        assertThat(viewerCreation).isInstanceOf(SpaceAccessDeniedException.class);
    }

    @Test
    void strangerGetsNotFoundForAnotherSpace() {
        // given
        SpaceService service = service();
        Space space = seedSpace("stranger");
        String subject = uniqueSubject("stranger-target");

        // when
        Throwable read = catchThrowable(() -> service.view(STRANGER, space.slug()));
        Throwable add = catchThrowable(() -> service.addMember(STRANGER, space.slug(), subject, SpaceRole.VIEWER));
        Throwable change = catchThrowable(() -> service.changeRole(STRANGER, space.slug(), subject, SpaceRole.EDITOR));
        Throwable remove = catchThrowable(() -> service.removeMember(STRANGER, space.slug(), subject));
        Throwable write = catchThrowable(() -> service.put(STRANGER, uniqueSlug("stranger-write"), "Denied"));
        List<SpaceView> visible = service.listVisible(STRANGER, null, 100);

        // then
        assertThat(read).isInstanceOf(SpaceNotFoundException.class);
        assertThat(add).isInstanceOf(SpaceNotFoundException.class);
        assertThat(change).isInstanceOf(SpaceNotFoundException.class);
        assertThat(remove).isInstanceOf(SpaceNotFoundException.class);
        assertThat(write).isInstanceOf(SpaceAccessDeniedException.class);
        assertThat(visible).isEmpty();
    }

    @Test
    void viewerReadsButCannotWrite() {
        // given
        SpaceService service = service();
        Space space = seedSpace("viewer");
        String slug = space.slug();

        // when
        SpaceView read = service.view(VIEWER, slug);
        List<SpaceView> visible = service.listVisible(VIEWER, null, 100);
        Throwable write = catchThrowable(() -> service.put(VIEWER, uniqueSlug("viewer-write"), "Denied"));
        Throwable add =
                catchThrowable(() -> service.addMember(VIEWER, slug, uniqueSubject("viewer-target"), SpaceRole.VIEWER));
        Throwable change = catchThrowable(() -> service.changeRole(VIEWER, slug, EDITOR.subject(), SpaceRole.VIEWER));
        Throwable remove = catchThrowable(() -> service.removeMember(VIEWER, slug, EDITOR.subject()));

        // then
        assertThat(read.id()).isEqualTo(space.id());
        assertThat(visible).extracting(SpaceView::slug).contains(slug);
        assertThat(write).isInstanceOf(SpaceAccessDeniedException.class);
        assertThat(add).isInstanceOf(SpaceAccessDeniedException.class);
        assertThat(change).isInstanceOf(SpaceAccessDeniedException.class);
        assertThat(remove).isInstanceOf(SpaceAccessDeniedException.class);
    }

    @Test
    void editorManagesMembersOfOwnSpaceOnly() {
        // given
        SpaceService service = service();
        Space ownSpace = seedSpace("editor-own");
        String foreignSlug = uniqueSlug("editor-foreign");
        Space foreignSpace = service.put(ADMIN, foreignSlug, "Foreign space").space();
        String ownSlug = ownSpace.slug();
        String externalForeignSlug = foreignSpace.slug();
        String subject = uniqueSubject("managed");
        SpacePrincipal member = new SpacePrincipal(subject, false);

        // when
        service.addMember(EDITOR, ownSlug, subject, SpaceRole.VIEWER);
        List<SpaceView> afterAdd = service.listVisible(member, null, 100);
        service.changeRole(EDITOR, ownSlug, subject, SpaceRole.EDITOR);
        List<SpaceView> afterChange = service.listVisible(member, null, 100);
        Throwable foreignAdd =
                catchThrowable(() -> service.addMember(EDITOR, externalForeignSlug, subject, SpaceRole.VIEWER));
        Throwable foreignChange =
                catchThrowable(() -> service.changeRole(EDITOR, externalForeignSlug, subject, SpaceRole.EDITOR));
        Throwable foreignRemove = catchThrowable(() -> service.removeMember(EDITOR, externalForeignSlug, subject));
        service.removeMember(EDITOR, ownSlug, subject);
        List<SpaceView> afterRemove = service.listVisible(member, null, 100);

        // then
        assertThat(afterAdd).extracting(SpaceView::slug).contains(ownSlug);
        assertThat(afterChange).extracting(SpaceView::role).contains(SpaceRole.EDITOR);
        assertThat(afterRemove).isEmpty();
        assertThat(foreignAdd).isInstanceOf(SpaceNotFoundException.class);
        assertThat(foreignChange).isInstanceOf(SpaceNotFoundException.class);
        assertThat(foreignRemove).isInstanceOf(SpaceNotFoundException.class);
    }

    @Test
    void adminSeesEverySpaceAndMembersOnlyTheirs() {
        // given
        SpaceService service = service();
        Space memberSpace = seedSpace("visible");
        Space foreignSpace =
                service.put(ADMIN, uniqueSlug("hidden"), "Hidden space").space();
        String memberSlug = memberSpace.slug();
        String foreignSlug = foreignSpace.slug();

        // when
        List<SpaceView> adminVisible = service.listVisible(ADMIN, null, 1000);
        List<SpaceView> editorVisible = service.listVisible(EDITOR, null, 1000);
        List<SpaceView> viewerVisible = service.listVisible(VIEWER, null, 1000);
        SpaceView adminRead = service.view(ADMIN, foreignSlug);
        Throwable editorRead = catchThrowable(() -> service.view(EDITOR, foreignSlug));

        // then
        assertThat(adminVisible).extracting(SpaceView::slug).contains(memberSlug, foreignSlug);
        assertThat(editorVisible)
                .extracting(SpaceView::slug)
                .contains(memberSlug)
                .doesNotContain(foreignSlug);
        assertThat(viewerVisible)
                .extracting(SpaceView::slug)
                .contains(memberSlug)
                .doesNotContain(foreignSlug);
        assertThat(adminRead.id()).isEqualTo(foreignSpace.id());
        assertThat(editorRead).isInstanceOf(SpaceNotFoundException.class);

        SpaceView adminOwnView = viewOf(adminVisible, memberSlug);
        SpaceView adminForeignView = viewOf(adminVisible, foreignSlug);
        SpaceView editorView = viewOf(editorVisible, memberSlug);
        SpaceView viewerView = viewOf(viewerVisible, memberSlug);
        assertThat(adminOwnView.admin()).isTrue();
        assertThat(adminOwnView.role()).isNull();
        assertThat(adminForeignView.admin()).isTrue();
        assertThat(editorView.admin()).isFalse();
        assertThat(editorView.role()).isEqualTo(SpaceRole.EDITOR);
        assertThat(viewerView.role()).isEqualTo(SpaceRole.VIEWER);
    }

    @Test
    void viewReturnsOneSpaceWithTheCallersRole() {
        // given
        SpaceService service = service();
        Space space = seedSpace("view-one");
        Space foreignSpace =
                service.put(ADMIN, uniqueSlug("view-hidden"), "Hidden view").space();
        String slug = space.slug();
        String foreignSlug = foreignSpace.slug();

        // when
        SpaceView adminView = service.view(ADMIN, slug);
        SpaceView adminForeignView = service.view(ADMIN, foreignSlug);
        SpaceView editorView = service.view(EDITOR, slug);
        SpaceView viewerView = service.view(VIEWER, slug);
        Throwable editorForeign = catchThrowable(() -> service.view(EDITOR, foreignSlug));
        Throwable strangerView = catchThrowable(() -> service.view(STRANGER, slug));

        // then
        assertThat(adminView.id()).isEqualTo(space.id());
        assertThat(adminView.admin()).isTrue();
        assertThat(adminView.role()).isNull();
        assertThat(adminForeignView.slug()).isEqualTo(foreignSlug);
        assertThat(adminForeignView.admin()).isTrue();
        assertThat(editorView.admin()).isFalse();
        assertThat(editorView.role()).isEqualTo(SpaceRole.EDITOR);
        assertThat(viewerView.role()).isEqualTo(SpaceRole.VIEWER);
        assertThat(editorForeign).isInstanceOf(SpaceNotFoundException.class);
        assertThat(strangerView).isInstanceOf(SpaceNotFoundException.class);
    }

    @Test
    void invalidNamesAreRejected() {
        // given
        SpaceService service = service();
        String tooLongName = "x".repeat(101);
        String longSlug = uniqueSlug("name-long");
        String blankSlug = uniqueSlug("name-blank");

        // when
        Throwable blankName = catchThrowable(() -> service.put(ADMIN, blankSlug, "   "));
        Throwable longName = catchThrowable(() -> service.put(ADMIN, longSlug, tooLongName));

        // then
        assertThat(blankName).isInstanceOf(InvalidSpaceException.class);
        assertThat(longName).isInstanceOf(InvalidSpaceException.class);
        InvalidSpaceException exception = (InvalidSpaceException) longName;
        assertThat(exception.field()).isEqualTo("name");
        assertThat(exception.code()).isEqualTo("invalid_name");
    }

    @Test
    void changingARoleInvalidatesTheUsersSessions() {
        // given
        SpaceService service = service();
        Space space = seedSpace("role-session");
        String subject = uniqueSubject("role-change");
        String bystander = uniqueSubject("bystander");
        service.addMember(ADMIN, space.slug(), subject, SpaceRole.VIEWER);
        Session changedSession = createSession(subject);
        Session bystanderSession = createSession(bystander);

        // when
        service.changeRole(ADMIN, space.slug(), subject, SpaceRole.EDITOR);

        // then
        assertThat(sessionExists(changedSession)).isFalse();
        assertThat(sessionExists(bystanderSession)).isTrue();
    }

    @Test
    void removingAMemberInvalidatesTheUsersSessions() {
        // given
        SpaceService service = service();
        Space space = seedSpace("remove-session");
        String subject = uniqueSubject("removed");
        String bystander = uniqueSubject("kept");
        service.addMember(ADMIN, space.slug(), subject, SpaceRole.VIEWER);
        Session removedSession = createSession(subject);
        Session bystanderSession = createSession(bystander);

        // when
        service.removeMember(ADMIN, space.slug(), subject);

        // then
        assertThat(sessionExists(removedSession)).isFalse();
        assertThat(sessionExists(bystanderSession)).isTrue();
    }

    @Test
    void addingAMemberKeepsSessions() {
        // given
        SpaceService service = service();
        Space space = seedSpace("add-session");
        String subject = uniqueSubject("added");
        Session session = createSession(subject);

        // when
        service.addMember(ADMIN, space.slug(), subject, SpaceRole.VIEWER);
        SpacePrincipal member = new SpacePrincipal(subject, false);
        List<SpaceView> visible = service.listVisible(member, null, 100);

        // then
        assertThat(sessionExists(session)).isTrue();
        assertThat(visible).extracting(SpaceView::slug).contains(space.slug());
    }

    @Test
    void concurrentPutOfTheSameSlugCreatesOneSpace() throws Exception {
        // given
        SpaceService service = service();
        String slug = uniqueSlug("concurrent");
        String name = "Concurrent space";
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<PutResult> results = new ArrayList<>();

        // when
        try {
            Callable<PutResult> put = () -> service.put(ADMIN, slug, name);
            List<Future<PutResult>> futures = executor.invokeAll(List.of(put, put));
            for (Future<PutResult> future : futures) {
                results.add(future.get());
            }
        } finally {
            executor.shutdown();
        }

        // then
        long created = results.stream().filter(PutResult::created).count();
        List<SpaceView> visible = service.listVisible(ADMIN, null, 1000);
        long stored = visible.stream().filter(view -> view.slug().equals(slug)).count();
        assertThat(created).isEqualTo(1L);
        assertThat(stored).isEqualTo(1L);
    }

    @Test
    void listVisibleIsOrderedBySlugAndResumesAfterASlug() {
        // given
        SpaceService service = service();
        seedSpace("list");
        seedSpace("list");
        seedSpace("list");

        // when
        List<SpaceView> all = service.listVisible(ADMIN, null, 1000);
        SpaceView first = all.get(0);
        List<SpaceView> afterFirst = service.listVisible(ADMIN, first.slug(), 1000);

        // then
        assertThat(all).extracting(SpaceView::slug).isSorted();
        List<SpaceView> expectedTail = all.subList(1, all.size());
        assertThat(afterFirst).isEqualTo(expectedTail);
    }

    private SpaceService service() {
        return bean(SpaceService.class);
    }

    private Space seedSpace(String prefix) {
        SpaceService service = service();
        PutResult created = service.put(ADMIN, uniqueSlug(prefix), "Space " + prefix);
        Space space = created.space();
        String slug = space.slug();
        service.addMember(ADMIN, slug, EDITOR.subject(), SpaceRole.EDITOR);
        service.addMember(ADMIN, slug, VIEWER.subject(), SpaceRole.VIEWER);
        return space;
    }

    private static SpaceView viewOf(List<SpaceView> views, String slug) {
        return views.stream()
                .filter(view -> view.slug().equals(slug))
                .findFirst()
                .orElseThrow();
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

    private static String uniqueSlug(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String uniqueSubject(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
