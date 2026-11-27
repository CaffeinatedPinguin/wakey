package dev.wakey.spaces;

import dev.wakey.spaces.internal.SpaceEntity;
import dev.wakey.spaces.internal.SpaceMemberEntity;
import dev.wakey.spaces.internal.SpaceStore;
import dev.wakey.web.ApiException;
import dev.wakey.web.CommonError;
import dev.wakey.web.CurrentUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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

    /**
     * Creates a space or renames it by slug. A repeat with the same name and the current version (or
     * no version) changes nothing while every other write bumps the version; a stale {@code
     * expectedVersion} is a conflict.
     */
    public PutResult put(
            CurrentUser actor, @InvalidSlug String slug, @InvalidName String name, @Nullable Long expectedVersion) {
        actor.requireAdmin();
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
        actor.requireAdmin();
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
        SpaceMemberId caller = memberIdOf(actor);
        Optional<SpaceMemberEntity> membership = store.findMember(space.id(), caller);
        if (!actor.admin() && membership.isEmpty()) {
            throw new ApiException(CommonError.NOT_FOUND);
        }
        @Nullable SpaceMemberEntity member = membership.orElse(null);
        return store.toView(space, actor.admin(), member);
    }

    /** Reads one space as a non-admin member with the given identity; used by other modules. */
    public SpaceView viewForMember(String issuer, String subject, String slug) {
        return view(new CurrentUser(issuer, subject, false), slug);
    }

    /** Reads one space as a non-admin member by id; a missing space or a non-member is empty. */
    public Optional<SpaceView> viewForMemberById(UUID spaceId, String issuer, String subject) {
        Optional<SpaceEntity> found = store.findById(spaceId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        SpaceEntity space = found.get();
        SpaceMemberId caller = new SpaceMemberId(issuer, subject);
        return store.findMember(space.id(), caller).map(member -> store.toView(space, false, member));
    }

    /** Reads one space for a write: a stranger gets 404 and a viewer gets 403. */
    public SpaceView viewForWrite(CurrentUser actor, @InvalidSlug String slug) {
        SpaceView view = view(actor, slug);
        if (!view.canWrite()) {
            throw new ApiException(CommonError.FORBIDDEN);
        }
        return view;
    }

    /** Whether a space with the slug exists; the upsert uses it before the body is validated. */
    public boolean exists(String slug) {
        return store.findBySlug(slug).isPresent();
    }

    /** Returns the slug of a space by id, or empty when there is no such space. */
    public Optional<String> slugOf(UUID spaceId) {
        return store.findSlug(spaceId);
    }

    /** Returns the assigned plan name of a space by id; empty means the instance default plan. */
    public Optional<String> planNameOf(UUID spaceId) {
        return store.findById(spaceId).flatMap(space -> Optional.ofNullable(space.planName()));
    }

    /** Locks one space row until the surrounding transaction ends; a missing space is not found. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(UUID spaceId) {
        if (store.lock(spaceId).isEmpty()) {
            throw new ApiException(CommonError.NOT_FOUND);
        }
    }

    /** Lists the spaces the caller can see, ordered by slug and resuming after the given slug. */
    public List<SpaceView> listVisible(CurrentUser actor, @Nullable String afterSlug, int limit) {
        String cursor = afterSlug == null ? "" : afterSlug;
        SpaceMemberId caller = memberIdOf(actor);
        if (actor.admin()) {
            return store.listAll(caller, cursor, limit, true);
        }
        return store.listForMember(caller, cursor, limit);
    }

    /** Stores the plan of a space; the caller must be an application Admin. Repeating it changes nothing. */
    public void assignPlan(CurrentUser actor, @InvalidSlug String slug, String planName) {
        actor.requireAdmin();
        SpaceEntity space = requireSpace(slug);
        if (!planName.equals(space.planName())) {
            store.updatePlan(space, planName);
        }
    }

    private PutResult keepOrRename(SpaceEntity existing, String name, @Nullable Long expectedVersion) {
        if (nothingChanges(existing, name, expectedVersion)) {
            return new PutResult(store.toSpace(existing), false);
        }
        long version = expectedVersion == null ? existing.version() : expectedVersion;
        SpaceEntity renamed = store.rename(existing, name, version);
        return new PutResult(store.toSpace(renamed), false);
    }

    private static boolean nothingChanges(SpaceEntity existing, String name, @Nullable Long expectedVersion) {
        boolean sameName = existing.name().equals(name);
        boolean currentVersion = expectedVersion == null || expectedVersion == existing.version();
        return sameName && currentVersion;
    }

    private SpaceEntity requireSpace(String slug) {
        return store.findBySlug(slug).orElseThrow(() -> new ApiException(CommonError.NOT_FOUND));
    }

    private static SpaceMemberId memberIdOf(CurrentUser actor) {
        String issuer = actor.issuer();
        String subject = actor.subject();
        return new SpaceMemberId(issuer, subject);
    }

    private static ApiException slugTaken() {
        return ApiException.withField(SpaceError.SLUG_TAKEN, "/slug");
    }
}
