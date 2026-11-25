package dev.wakey.spaces;

import dev.wakey.security.CurrentUser;
import dev.wakey.spaces.internal.SpaceEntity;
import dev.wakey.spaces.internal.SpaceStore;
import dev.wakey.web.ApiException;
import dev.wakey.web.CommonError;
import dev.wakey.web.FieldProblem;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The spaces use cases. It owns the access rules and the API errors; persistence lives in the
 * module's internal Spring Data JDBC adapter.
 */
@Service
@RequiredArgsConstructor
public class SpaceService {

    public static final int MAX_SUBJECT_LENGTH = 100;

    private final SpaceStore store;

    private final UserSessions sessions;

    /** Creates a space or renames it by slug without a version check. */
    public PutResult put(CurrentUser actor, String slug, String name) {
        return put(actor, slug, name, null);
    }

    /**
     * Creates a space or renames it by slug. A repeat with the same name changes nothing while a
     * different name bumps the version; a stale {@code expectedVersion} is a conflict.
     */
    public PutResult put(CurrentUser actor, String slug, String name, @Nullable Long expectedVersion) {
        requireAdmin(actor);
        String checkedSlug = SpaceSlug.requireValid(slug);
        String checkedName = SpaceName.requireValid(name);
        Optional<SpaceEntity> found = store.findBySlug(checkedSlug);
        if (found.isEmpty()) {
            Optional<SpaceEntity> inserted = store.insert(checkedSlug, checkedName);
            if (inserted.isPresent()) {
                SpaceEntity created = inserted.get();
                return new PutResult(store.toSpace(created), true);
            }
            found = store.findBySlug(checkedSlug);
        }
        SpaceEntity existing = found.orElseThrow(() -> new ApiException(CommonError.NOT_FOUND));
        return keepOrRename(existing, checkedName, expectedVersion);
    }

    /** Creates a space only when its slug is still free; an existing name is never changed. */
    public PutResult create(CurrentUser actor, String slug, String name) {
        requireAdmin(actor);
        String checkedSlug = SpaceSlug.requireValid(slug);
        String checkedName = SpaceName.requireValid(name);
        Optional<SpaceEntity> inserted = store.insert(checkedSlug, checkedName);
        if (inserted.isEmpty()) {
            throw slugTaken();
        }
        SpaceEntity created = inserted.get();
        return new PutResult(store.toSpace(created), true);
    }

    /** Reads one space together with the caller's role; a non-member non-admin gets 404. */
    public SpaceView view(CurrentUser actor, String slug) {
        String checkedSlug = SpaceSlug.requireValid(slug);
        SpaceEntity space = requireSpace(checkedSlug);
        Optional<SpaceRole> role = store.findRole(space.id(), actor.subject());
        if (!actor.admin() && role.isEmpty()) {
            throw new ApiException(CommonError.NOT_FOUND);
        }
        @Nullable SpaceRole callerRole = role.isPresent() ? role.get() : null;
        return store.toView(space, callerRole);
    }

    /** Lists the spaces the caller can see, ordered by slug and resuming after the given slug. */
    public List<SpaceView> listVisible(CurrentUser actor, @Nullable String afterSlug, int limit) {
        String cursor = afterSlug == null ? "" : afterSlug;
        if (actor.admin()) {
            return store.listAll(actor.subject(), cursor, limit);
        }
        return store.listForMember(actor.subject(), cursor, limit);
    }

    /** Adds a member to a space the caller manages; an existing membership keeps its role. */
    public void addMember(CurrentUser actor, String slug, String subject, SpaceRole role) {
        requireValidSubject(subject);
        SpaceEntity space = requireManageableSpace(actor, slug);
        store.addMember(space.id(), subject, role);
    }

    /** Changes a member's role and invalidates that user's sessions when the role really changes. */
    @Transactional
    public void changeRole(CurrentUser actor, String slug, String subject, SpaceRole role) {
        requireValidSubject(subject);
        SpaceEntity space = requireManageableSpace(actor, slug);
        Optional<SpaceRole> current = store.findRole(space.id(), subject);
        SpaceRole currentRole = current.orElseThrow(() -> new ApiException(CommonError.NOT_FOUND));
        if (currentRole != role) {
            store.updateRole(space.id(), subject, role);
            sessions.invalidateAll(subject);
        }
    }

    /** Removes a member and invalidates that user's sessions. */
    @Transactional
    public void removeMember(CurrentUser actor, String slug, String subject) {
        requireValidSubject(subject);
        SpaceEntity space = requireManageableSpace(actor, slug);
        if (!store.removeMember(space.id(), subject)) {
            throw new ApiException(CommonError.NOT_FOUND);
        }
        sessions.invalidateAll(subject);
    }

    private PutResult keepOrRename(SpaceEntity existing, String name, @Nullable Long expectedVersion) {
        requireMatchingVersion(existing, expectedVersion);
        if (existing.name().equals(name)) {
            return new PutResult(store.toSpace(existing), false);
        }
        SpaceEntity renamed = rename(existing, name);
        return new PutResult(store.toSpace(renamed), false);
    }

    private SpaceEntity rename(SpaceEntity existing, String name) {
        try {
            return store.rename(existing, name);
        } catch (OptimisticLockingFailureException exception) {
            throw new ApiException(SpaceError.VERSION_CONFLICT, exception);
        }
    }

    private static void requireMatchingVersion(SpaceEntity existing, @Nullable Long expectedVersion) {
        if (expectedVersion != null && existing.version() != expectedVersion) {
            throw new ApiException(SpaceError.VERSION_CONFLICT);
        }
    }

    private SpaceEntity requireManageableSpace(CurrentUser actor, String slug) {
        String checkedSlug = SpaceSlug.requireValid(slug);
        SpaceEntity space = requireSpace(checkedSlug);
        if (!actor.admin()) {
            requireManager(space, actor.subject());
        }
        return space;
    }

    private void requireManager(SpaceEntity space, String subject) {
        Optional<SpaceRole> role = store.findRole(space.id(), subject);
        if (role.isEmpty()) {
            throw new ApiException(CommonError.NOT_FOUND);
        }
        if (role.get() == SpaceRole.VIEWER) {
            throw new ApiException(CommonError.FORBIDDEN);
        }
    }

    private SpaceEntity requireSpace(String slug) {
        return store.findBySlug(slug).orElseThrow(() -> new ApiException(CommonError.NOT_FOUND));
    }

    private static ApiException slugTaken() {
        FieldProblem problem = new FieldProblem("/slug", SpaceError.SLUG_TAKEN.getCode(), Map.of());
        return new ApiException(SpaceError.SLUG_TAKEN, List.of(problem));
    }

    private static void requireAdmin(CurrentUser actor) {
        if (!actor.admin()) {
            throw new ApiException(CommonError.FORBIDDEN);
        }
    }

    private static void requireValidSubject(String subject) {
        if (subject.isBlank() || subject.length() > MAX_SUBJECT_LENGTH) {
            FieldProblem problem = new FieldProblem("/subject", SpaceError.INVALID_SUBJECT.getCode(), Map.of());
            throw new ApiException(CommonError.VALIDATION_FAILED, List.of(problem));
        }
    }
}
