package dev.wakey.spaces;

import dev.wakey.security.CurrentUser;
import dev.wakey.spaces.internal.SpaceEntity;
import dev.wakey.spaces.internal.SpaceStore;
import dev.wakey.web.ApiException;
import dev.wakey.web.CommonError;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * The spaces use cases. It owns the access rules and the API errors; persistence lives in the
 * module's internal Spring Data JDBC adapter.
 */
@Validated
@Service
@RequiredArgsConstructor
public class SpaceService {

    private final SpaceStore store;

    private final UserSessions sessions;

    /** Creates a space or renames it by slug without a version check. */
    public PutResult put(CurrentUser actor, @InvalidSlug String slug, @InvalidName String name) {
        return put(actor, slug, name, null);
    }

    /**
     * Creates a space or renames it by slug. A repeat with the same name changes nothing while a
     * different name bumps the version; a stale {@code expectedVersion} is a conflict.
     */
    public PutResult put(
            CurrentUser actor, @InvalidSlug String slug, @InvalidName String name, @Nullable Long expectedVersion) {
        requireAdmin(actor);
        String checkedName = SpaceName.normalize(name);
        Optional<SpaceEntity> found = store.findBySlug(slug);
        if (found.isEmpty()) {
            Optional<SpaceEntity> inserted = store.insert(slug, checkedName);
            if (inserted.isPresent()) {
                SpaceEntity created = inserted.get();
                return new PutResult(store.toSpace(created), true);
            }
            found = store.findBySlug(slug);
        }
        SpaceEntity existing = found.orElseThrow(() -> new ApiException(CommonError.NOT_FOUND));
        return keepOrRename(existing, checkedName, expectedVersion);
    }

    /** Creates a space only when its slug is still free; an existing name is never changed. */
    public PutResult create(CurrentUser actor, @InvalidSlug String slug, @InvalidName String name) {
        requireAdmin(actor);
        String checkedName = SpaceName.normalize(name);
        Optional<SpaceEntity> inserted = store.insert(slug, checkedName);
        if (inserted.isEmpty()) {
            throw slugTaken();
        }
        SpaceEntity created = inserted.get();
        return new PutResult(store.toSpace(created), true);
    }

    /** Reads one space together with the caller's role; a non-member non-admin gets 404. */
    public SpaceView view(CurrentUser actor, @InvalidSlug String slug) {
        SpaceEntity space = requireSpace(slug);
        Optional<SpaceRole> role = store.findRole(space.id(), actor.issuer(), actor.subject());
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
            return store.listAll(actor.issuer(), actor.subject(), cursor, limit);
        }
        return store.listForMember(actor.issuer(), actor.subject(), cursor, limit);
    }

    /** Adds a member to a space the caller manages; an existing membership keeps its role. */
    public void addMember(CurrentUser actor, @InvalidSlug String slug, @Valid SpaceMember member) {
        SpaceEntity space = requireManageableSpace(actor, slug);
        store.addMember(space.id(), member);
    }

    /** Changes a member's role and invalidates that user's sessions when the role really changes. */
    @Transactional
    public void changeRole(CurrentUser actor, @InvalidSlug String slug, @Valid SpaceMember member) {
        SpaceEntity space = requireManageableSpace(actor, slug);
        Optional<SpaceRole> current = store.findRole(space.id(), member.issuer(), member.subject());
        SpaceRole currentRole = current.orElseThrow(() -> new ApiException(CommonError.NOT_FOUND));
        if (currentRole != member.role()) {
            store.updateRole(space.id(), member);
            sessions.invalidateAll(member.issuer(), member.subject());
        }
    }

    /** Removes a member and invalidates that user's sessions. */
    @Transactional
    public void removeMember(CurrentUser actor, @InvalidSlug String slug, @Valid SpaceMemberId member) {
        SpaceEntity space = requireManageableSpace(actor, slug);
        if (!store.removeMember(space.id(), member.issuer(), member.subject())) {
            throw new ApiException(CommonError.NOT_FOUND);
        }
        sessions.invalidateAll(member.issuer(), member.subject());
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
        SpaceEntity space = requireSpace(slug);
        if (!actor.admin()) {
            requireManager(space, actor);
        }
        return space;
    }

    private void requireManager(SpaceEntity space, CurrentUser actor) {
        Optional<SpaceRole> role = store.findRole(space.id(), actor.issuer(), actor.subject());
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
        return ApiException.withField(SpaceError.SLUG_TAKEN, "/slug");
    }

    private static void requireAdmin(CurrentUser actor) {
        if (!actor.admin()) {
            throw new ApiException(CommonError.FORBIDDEN);
        }
    }
}
