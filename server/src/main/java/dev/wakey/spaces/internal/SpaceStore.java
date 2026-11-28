package dev.wakey.spaces.internal;

import dev.wakey.spaces.AddMemberResult;
import dev.wakey.spaces.Space;
import dev.wakey.spaces.SpaceMember;
import dev.wakey.spaces.SpaceMemberId;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceView;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Limit;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The persistence adapter of the spaces aggregate: Spring Data JDBC repositories plus the aggregate
 * template. It returns domain records so the service keeps only the access rules.
 */
@Component
@RequiredArgsConstructor
public class SpaceStore {

    private final SpaceRepository spaces;

    private final SpaceMemberRepository members;

    private final JdbcAggregateTemplate aggregates;

    private final Clock clock;

    public Optional<SpaceEntity> findById(UUID spaceId) {
        return spaces.findById(spaceId);
    }

    public Optional<SpaceEntity> findBySlug(String slug) {
        return spaces.findBySlug(slug);
    }

    public Optional<String> findSlug(UUID spaceId) {
        return findById(spaceId).map(SpaceEntity::slug);
    }

    /** Locks one space row until the surrounding transaction ends; a missing row is empty. */
    public Optional<SpaceEntity> lock(UUID spaceId) {
        return spaces.findForUpdateById(spaceId);
    }

    /** Inserts a space in its own savepoint so a duplicate slug leaves the caller's transaction usable. */
    @Transactional(propagation = Propagation.NESTED)
    public SpaceEntity insert(String slug, String name) {
        Instant now = clock.instant();
        SpaceEntity entity = new SpaceEntity(UUID.randomUUID(), slug, name, null, 0L, now, now);
        return spaces.save(entity);
    }

    public SpaceEntity rename(SpaceEntity existing, String name, long version) {
        SpaceEntity renamed = new SpaceEntity(
                existing.id(),
                existing.slug(),
                name,
                existing.planName(),
                version,
                existing.createdAt(),
                clock.instant());
        return aggregates.update(renamed);
    }

    public SpaceEntity updatePlan(SpaceEntity existing, String planName) {
        SpaceEntity updated = new SpaceEntity(
                existing.id(),
                existing.slug(),
                existing.name(),
                planName,
                existing.version(),
                existing.createdAt(),
                clock.instant());
        return spaces.save(updated);
    }

    public Optional<SpaceMemberEntity> findMember(UUID spaceId, SpaceMemberId member) {
        SpaceMemberKey key = new SpaceMemberKey(spaceId, member.issuer(), member.subject());
        return members.findById(key);
    }

    public Optional<SpaceRole> findRole(UUID spaceId, SpaceMemberId member) {
        return findMember(spaceId, member).map(SpaceMemberEntity::role);
    }

    public List<SpaceView> listAll(SpaceMemberId caller, String cursor, int rowLimit, boolean admin) {
        List<SpaceEntity> page = spaces.findBySlugGreaterThanOrderBySlug(cursor, Limit.of(rowLimit));
        if (page.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = new ArrayList<>(page.size());
        for (SpaceEntity space : page) {
            ids.add(space.id());
        }
        Map<UUID, SpaceMemberEntity> memberships = membershipsOf(ids, caller);
        return viewsOf(page, memberships, admin);
    }

    /**
     * Lists a member's spaces in two queries per page: it loads all memberships of the one user
     * (that user's spaces are few) and then pages the spaces by slug.
     */
    public List<SpaceView> listForMember(SpaceMemberId caller, String cursor, int rowLimit) {
        String issuer = caller.issuer();
        String subject = caller.subject();
        List<SpaceMemberEntity> memberships = members.findByKeyIssuerAndKeySubject(issuer, subject);
        if (memberships.isEmpty()) {
            return List.of();
        }
        Map<UUID, SpaceMemberEntity> bySpace = new HashMap<>();
        List<UUID> ids = new ArrayList<>(memberships.size());
        for (SpaceMemberEntity membership : memberships) {
            SpaceMemberKey key = membership.key();
            bySpace.put(key.spaceId(), membership);
            ids.add(key.spaceId());
        }
        List<SpaceEntity> page = spaces.findByIdInAndSlugGreaterThanOrderBySlug(ids, cursor, Limit.of(rowLimit));
        return viewsOf(page, bySpace, false);
    }

    /** Adds a membership in its own savepoint so a concurrent duplicate leaves the caller's transaction usable. */
    @Transactional(propagation = Propagation.NESTED)
    public AddMemberResult addMember(UUID spaceId, SpaceMember member) {
        SpaceMemberKey key = new SpaceMemberKey(spaceId, member.issuer(), member.subject());
        Optional<SpaceMemberEntity> existing = members.findById(key);
        if (existing.isPresent()) {
            SpaceMember stored = toMember(existing.get());
            return new AddMemberResult(stored, false);
        }
        aggregates.insert(new SpaceMemberEntity(key, member.role()));
        return new AddMemberResult(member, true);
    }

    /** Lists members ordered by issuer and subject, resuming after the given identity. */
    public List<SpaceMember> listMembers(UUID spaceId, SpaceMemberId after, int rowLimit) {
        List<SpaceMemberEntity> sameIssuer =
                members.findByKeySpaceIdAndKeyIssuerAndKeySubjectGreaterThanOrderByKeySubjectAsc(
                        spaceId, after.issuer(), after.subject(), Limit.of(rowLimit));
        List<SpaceMemberEntity> laterIssuers =
                members.findByKeySpaceIdAndKeyIssuerGreaterThanOrderByKeyIssuerAscKeySubjectAsc(
                        spaceId, after.issuer(), Limit.of(rowLimit));
        List<SpaceMember> page = new ArrayList<>(rowLimit);
        addMembers(page, sameIssuer, rowLimit);
        addMembers(page, laterIssuers, rowLimit);
        return page;
    }

    private static void addMembers(List<SpaceMember> target, List<SpaceMemberEntity> entities, int rowLimit) {
        for (SpaceMemberEntity entity : entities) {
            if (target.size() >= rowLimit) {
                return;
            }
            target.add(toMember(entity));
        }
    }

    public void updateRole(UUID spaceId, SpaceMember member) {
        SpaceMemberKey key = new SpaceMemberKey(spaceId, member.issuer(), member.subject());
        SpaceMemberEntity existing = members.findById(key).orElseThrow();
        SpaceRole role = member.role();
        members.save(new SpaceMemberEntity(existing.key(), role));
    }

    public boolean removeMember(UUID spaceId, SpaceMemberId member) {
        SpaceMemberKey key = new SpaceMemberKey(spaceId, member.issuer(), member.subject());
        if (members.findById(key).isEmpty()) {
            return false;
        }
        members.deleteById(key);
        return true;
    }

    public Space toSpace(SpaceEntity entity) {
        return new Space(entity.id(), entity.slug(), entity.name(), entity.planName(), entity.version());
    }

    public static SpaceView toView(SpaceEntity entity, boolean admin, @Nullable SpaceMemberEntity member) {
        @Nullable SpaceRole role = member == null ? null : member.role();
        return new SpaceView(
                entity.id(), entity.slug(), entity.name(), entity.planName(), entity.version(), admin, role);
    }

    private static SpaceMember toMember(SpaceMemberEntity entity) {
        SpaceMemberKey key = entity.key();
        return new SpaceMember(key.issuer(), key.subject(), entity.role());
    }

    private Map<UUID, SpaceMemberEntity> membershipsOf(List<UUID> spaceIds, SpaceMemberId caller) {
        String issuer = caller.issuer();
        String subject = caller.subject();
        List<SpaceMemberEntity> found = members.findByKeySpaceIdInAndKeyIssuerAndKeySubject(spaceIds, issuer, subject);
        Map<UUID, SpaceMemberEntity> memberships = new HashMap<>();
        for (SpaceMemberEntity member : found) {
            SpaceMemberKey key = member.key();
            memberships.put(key.spaceId(), member);
        }
        return memberships;
    }

    private static List<SpaceView> viewsOf(
            List<SpaceEntity> page, Map<UUID, SpaceMemberEntity> memberships, boolean admin) {
        List<SpaceView> views = new ArrayList<>(page.size());
        for (SpaceEntity space : page) {
            @Nullable SpaceMemberEntity member = memberships.get(space.id());
            views.add(toView(space, admin, member));
        }
        return views;
    }
}
