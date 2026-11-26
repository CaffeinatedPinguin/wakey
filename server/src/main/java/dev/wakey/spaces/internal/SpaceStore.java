package dev.wakey.spaces.internal;

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
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Limit;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.stereotype.Component;

/**
 * The persistence adapter of the spaces aggregate: Spring Data JDBC repositories plus the aggregate
 * template. It returns domain records so the service keeps only the access rules.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpaceStore {

    private final SpaceRepository spaces;

    private final SpaceMemberRepository members;

    private final JdbcAggregateTemplate aggregates;

    private final Clock clock;

    public Optional<SpaceEntity> findBySlug(String slug) {
        return spaces.findBySlug(slug);
    }

    public Optional<SpaceEntity> insert(String slug, String name) {
        Instant now = clock.instant();
        SpaceEntity entity = new SpaceEntity(UUID.randomUUID(), slug, name, 0L, now, now);
        try {
            return Optional.of(spaces.save(entity));
        } catch (DuplicateKeyException exception) {
            log.debug("A space with slug {} already exists", slug, exception);
            return Optional.empty();
        }
    }

    public SpaceEntity rename(SpaceEntity existing, String name) {
        SpaceEntity renamed = new SpaceEntity(
                existing.id(), existing.slug(), name, existing.version(), existing.createdAt(), clock.instant());
        return spaces.save(renamed);
    }

    public Optional<SpaceRole> findRole(UUID spaceId, String issuer, String subject) {
        SpaceMemberKey key = new SpaceMemberKey(spaceId, issuer, subject);
        return members.findById(key).map(SpaceMemberEntity::role);
    }

    public List<SpaceView> listAll(String issuer, String subject, String cursor, int rowLimit) {
        List<SpaceEntity> page = spaces.findBySlugGreaterThanOrderBySlug(cursor, Limit.of(rowLimit));
        if (page.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = new ArrayList<>(page.size());
        for (SpaceEntity space : page) {
            ids.add(space.id());
        }
        Map<UUID, SpaceRole> roles = rolesOf(ids, issuer, subject);
        return viewsOf(page, roles);
    }

    /**
     * Lists a member's spaces in two queries per page: it loads all memberships of the one user
     * (that user's spaces are few) and then pages the spaces by slug.
     */
    public List<SpaceView> listForMember(String issuer, String subject, String cursor, int rowLimit) {
        List<SpaceMemberEntity> memberships = members.findByKeyIssuerAndKeySubject(issuer, subject);
        if (memberships.isEmpty()) {
            return List.of();
        }
        Map<UUID, SpaceRole> roles = new HashMap<>();
        List<UUID> ids = new ArrayList<>(memberships.size());
        for (SpaceMemberEntity membership : memberships) {
            SpaceMemberKey key = membership.key();
            roles.put(key.spaceId(), membership.role());
            ids.add(key.spaceId());
        }
        List<SpaceEntity> page = spaces.findByIdInAndSlugGreaterThanOrderBySlug(ids, cursor, Limit.of(rowLimit));
        return viewsOf(page, roles);
    }

    public SpaceMember addMember(UUID spaceId, SpaceMember member) {
        SpaceMemberKey key = new SpaceMemberKey(spaceId, member.issuer(), member.subject());
        Optional<SpaceMemberEntity> existing = members.findById(key);
        if (existing.isPresent()) {
            return toMember(existing.get());
        }
        insertMember(key, member.role());
        return members.findById(key).map(SpaceStore::toMember).orElse(member);
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

    private void insertMember(SpaceMemberKey key, SpaceRole role) {
        try {
            aggregates.insert(new SpaceMemberEntity(key, role));
        } catch (DuplicateKeyException exception) {
            log.debug("Membership {} already exists", key, exception);
        }
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
        members.save(new SpaceMemberEntity(key, member.role()));
    }

    public boolean removeMember(UUID spaceId, String issuer, String subject) {
        SpaceMemberKey key = new SpaceMemberKey(spaceId, issuer, subject);
        if (members.findById(key).isEmpty()) {
            return false;
        }
        members.deleteById(key);
        return true;
    }

    public Space toSpace(SpaceEntity entity) {
        return new Space(entity.id(), entity.slug(), entity.name(), entity.version());
    }

    public SpaceView toView(SpaceEntity entity, @Nullable SpaceRole role) {
        return new SpaceView(entity.id(), entity.slug(), entity.name(), entity.version(), role);
    }

    private static SpaceMember toMember(SpaceMemberEntity entity) {
        SpaceMemberKey key = entity.key();
        return new SpaceMember(key.issuer(), key.subject(), entity.role());
    }

    private Map<UUID, SpaceRole> rolesOf(List<UUID> spaceIds, String issuer, String subject) {
        List<SpaceMemberEntity> found = members.findByKeySpaceIdInAndKeyIssuerAndKeySubject(spaceIds, issuer, subject);
        Map<UUID, SpaceRole> roles = new HashMap<>();
        for (SpaceMemberEntity member : found) {
            SpaceMemberKey key = member.key();
            roles.put(key.spaceId(), member.role());
        }
        return roles;
    }

    private static List<SpaceView> viewsOf(List<SpaceEntity> page, Map<UUID, SpaceRole> roles) {
        List<SpaceView> views = new ArrayList<>(page.size());
        for (SpaceEntity space : page) {
            @Nullable SpaceRole role = roles.get(space.id());
            SpaceView view = new SpaceView(space.id(), space.slug(), space.name(), space.version(), role);
            views.add(view);
        }
        return views;
    }
}
