package dev.wakey.spaces.internal;

import dev.wakey.spaces.InvalidSpaceException;
import dev.wakey.spaces.PutResult;
import dev.wakey.spaces.SlugTakenException;
import dev.wakey.spaces.Space;
import dev.wakey.spaces.SpaceAccessDeniedException;
import dev.wakey.spaces.SpaceNotFoundException;
import dev.wakey.spaces.SpacePrincipal;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceService;
import dev.wakey.spaces.SpaceSlug;
import dev.wakey.spaces.SpaceView;
import dev.wakey.spaces.UserSessions;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
class JdbcSpaceService implements SpaceService {

    private static final int MAX_NAME_LENGTH = 100;

    private static final String FIND_SPACE_SQL = """
            SELECT id, slug, name, plan_name, version
            FROM space
            WHERE slug = :slug
            """;

    private static final String VIEW_SPACE_SQL = """
            SELECT s.id, s.slug, s.name, s.plan_name, s.version, m.role, m.since
            FROM space s
            LEFT JOIN space_member m ON m.space_id = s.id AND m.subject = :subject
            WHERE s.slug = :slug
            """;

    // postgres-only: ON CONFLICT (slug) DO NOTHING makes a concurrent create of the same slug safe.
    private static final String INSERT_SPACE_IF_ABSENT_SQL = """
            INSERT INTO space (id, slug, name, version, created_at, updated_at)
            VALUES (:id, :slug, :name, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (slug) DO NOTHING
            """;

    private static final String RENAME_SPACE_SQL = """
            UPDATE space
            SET name = :name, version = version + 1, updated_at = CURRENT_TIMESTAMP
            WHERE slug = :slug
            """;

    private static final String ASSIGN_PLAN_SQL = """
            UPDATE space
            SET plan_name = :planName, version = version + 1, updated_at = CURRENT_TIMESTAMP
            WHERE slug = :slug
            """;

    private static final String LIST_ALL_SPACES_SQL = """
            SELECT s.id, s.slug, s.name, s.plan_name, s.version, m.role, m.since
            FROM space s
            LEFT JOIN space_member m ON m.space_id = s.id AND m.subject = :subject
            WHERE s.slug > :afterSlug
            ORDER BY s.slug
            LIMIT :rowLimit
            """;

    private static final String LIST_MEMBER_SPACES_SQL = """
            SELECT s.id, s.slug, s.name, s.plan_name, s.version, m.role, m.since
            FROM space s
            JOIN space_member m ON m.space_id = s.id
            WHERE m.subject = :subject AND s.slug > :afterSlug
            ORDER BY s.slug
            LIMIT :rowLimit
            """;

    private static final String FIND_MEMBER_ROLE_SQL = """
            SELECT role
            FROM space_member
            WHERE space_id = :spaceId AND subject = :subject
            """;

    // postgres-only: ON CONFLICT (space_id, subject) DO NOTHING keeps an existing membership and its role.
    private static final String INSERT_MEMBER_IF_ABSENT_SQL = """
            INSERT INTO space_member (space_id, subject, role, since)
            VALUES (:spaceId, :subject, :role, :since)
            ON CONFLICT (space_id, subject) DO NOTHING
            """;

    private static final String UPDATE_MEMBER_ROLE_SQL = """
            UPDATE space_member
            SET role = :role
            WHERE space_id = :spaceId AND subject = :subject
            """;

    private static final String DELETE_MEMBER_SQL = """
            DELETE FROM space_member
            WHERE space_id = :spaceId AND subject = :subject
            """;

    private final JdbcClient jdbc;

    private final UserSessions sessions;

    private final Clock clock;

    @Override
    public PutResult put(SpacePrincipal actor, String slug, String name) {
        requireAdmin(actor);
        String checkedSlug = SpaceSlug.requireValid(slug);
        String checkedName = requireValidName(name);
        int inserted = insertSpaceIfAbsent(checkedSlug, checkedName);
        if (inserted == 1) {
            return new PutResult(findRequired(checkedSlug), true);
        }
        return keepOrRename(findRequired(checkedSlug), checkedName);
    }

    @Override
    public PutResult create(SpacePrincipal actor, String slug, String name) {
        requireAdmin(actor);
        String checkedSlug = SpaceSlug.requireValid(slug);
        String checkedName = requireValidName(name);
        int inserted = insertSpaceIfAbsent(checkedSlug, checkedName);
        if (inserted == 0) {
            throw new SlugTakenException(checkedSlug);
        }
        return new PutResult(findRequired(checkedSlug), true);
    }

    @Override
    public SpaceView view(SpacePrincipal actor, String slug) {
        String checkedSlug = SpaceSlug.requireValid(slug);
        boolean admin = actor.admin();
        Optional<SpaceView> found = jdbc.sql(VIEW_SPACE_SQL)
                .param("subject", actor.subject())
                .param("slug", checkedSlug)
                .query(SpaceRowMappers.view(admin))
                .optional();
        SpaceView view = found.orElseThrow(() -> new SpaceNotFoundException(checkedSlug));
        if (!actor.admin() && view.role() == null) {
            throw new SpaceNotFoundException(checkedSlug);
        }
        return view;
    }

    @Override
    public List<SpaceView> listVisible(SpacePrincipal actor, @Nullable String afterSlug, int limit) {
        String cursor = afterSlug == null ? "" : afterSlug;
        int rowLimit = Math.max(1, limit);
        if (actor.admin()) {
            return listSpaces(LIST_ALL_SPACES_SQL, actor, cursor, rowLimit);
        }
        return listSpaces(LIST_MEMBER_SPACES_SQL, actor, cursor, rowLimit);
    }

    @Override
    public void addMember(SpacePrincipal actor, String slug, String subject, SpaceRole role) {
        Space space = requireManageableSpace(actor, slug);
        OffsetDateTime since = OffsetDateTime.now(clock);
        jdbc.sql(INSERT_MEMBER_IF_ABSENT_SQL)
                .param("spaceId", space.id())
                .param("subject", subject)
                .param("role", role.name())
                .param("since", since)
                .update();
    }

    @Override
    @Transactional
    public void changeRole(SpacePrincipal actor, String slug, String subject, SpaceRole role) {
        Space space = requireManageableSpace(actor, slug);
        SpaceRole currentRole =
                findRole(space.id(), subject).orElseThrow(() -> new SpaceNotFoundException(space.slug()));
        if (currentRole != role) {
            updateMemberRole(space.id(), subject, role);
            sessions.invalidateAll(subject);
        }
    }

    @Override
    @Transactional
    public void removeMember(SpacePrincipal actor, String slug, String subject) {
        Space space = requireManageableSpace(actor, slug);
        int deleted = jdbc.sql(DELETE_MEMBER_SQL)
                .param("spaceId", space.id())
                .param("subject", subject)
                .update();
        if (deleted == 0) {
            throw new SpaceNotFoundException(space.slug());
        }
        sessions.invalidateAll(subject);
    }

    @Override
    public void assignPlan(SpacePrincipal actor, String slug, @Nullable String planName) {
        Space space = requireManageableSpace(actor, slug);
        requireAdmin(actor);
        if (!samePlan(space.planName(), planName)) {
            updatePlan(space.slug(), planName);
        }
    }

    private void updatePlan(String slug, @Nullable String planName) {
        jdbc.sql(ASSIGN_PLAN_SQL)
                .param("planName", planName)
                .param("slug", slug)
                .update();
    }

    private int insertSpaceIfAbsent(String slug, String name) {
        return jdbc.sql(INSERT_SPACE_IF_ABSENT_SQL)
                .param("id", UUID.randomUUID())
                .param("slug", slug)
                .param("name", name)
                .update();
    }

    private PutResult keepOrRename(Space existing, String name) {
        if (existing.name().equals(name)) {
            return new PutResult(existing, false);
        }
        Space renamed = rename(existing.slug(), name);
        return new PutResult(renamed, false);
    }

    private Space rename(String slug, String name) {
        jdbc.sql(RENAME_SPACE_SQL).param("name", name).param("slug", slug).update();
        return findRequired(slug);
    }

    private Space requireManageableSpace(SpacePrincipal actor, String slug) {
        String checkedSlug = SpaceSlug.requireValid(slug);
        Space space = findRequired(checkedSlug);
        if (!actor.admin()) {
            requireManager(space, actor);
        }
        return space;
    }

    private void requireManager(Space space, SpacePrincipal actor) {
        SpaceRole role =
                findRole(space.id(), actor.subject()).orElseThrow(() -> new SpaceNotFoundException(space.slug()));
        if (role == SpaceRole.VIEWER) {
            throw new SpaceAccessDeniedException("A viewer cannot manage members of space " + space.slug());
        }
    }

    private List<SpaceView> listSpaces(String sql, SpacePrincipal actor, String cursor, int rowLimit) {
        boolean admin = actor.admin();
        return jdbc.sql(sql)
                .param("subject", actor.subject())
                .param("afterSlug", cursor)
                .param("rowLimit", rowLimit)
                .query(SpaceRowMappers.view(admin))
                .list();
    }

    private Optional<Space> findSpace(String slug) {
        return jdbc.sql(FIND_SPACE_SQL)
                .param("slug", slug)
                .query(SpaceRowMappers.SPACE)
                .optional();
    }

    private Space findRequired(String slug) {
        return findSpace(slug).orElseThrow(() -> new SpaceNotFoundException(slug));
    }

    private Optional<SpaceRole> findRole(UUID spaceId, String subject) {
        return jdbc.sql(FIND_MEMBER_ROLE_SQL)
                .param("spaceId", spaceId)
                .param("subject", subject)
                .query(String.class)
                .optional()
                .map(SpaceRole::valueOf);
    }

    private void updateMemberRole(UUID spaceId, String subject, SpaceRole role) {
        jdbc.sql(UPDATE_MEMBER_ROLE_SQL)
                .param("role", role.name())
                .param("spaceId", spaceId)
                .param("subject", subject)
                .update();
    }

    private static boolean samePlan(@Nullable String current, @Nullable String requested) {
        return current == null ? requested == null : current.equals(requested);
    }

    private static void requireAdmin(SpacePrincipal actor) {
        if (!actor.admin()) {
            throw new SpaceAccessDeniedException("Only an administrator can create or rename spaces");
        }
    }

    private static String requireValidName(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH) {
            throw new InvalidSpaceException("name", "invalid_name", "A space name must have 1-100 characters");
        }
        return trimmed;
    }
}
