package dev.wakey.spaces.internal;

import dev.wakey.spaces.InvalidSpaceException;
import dev.wakey.spaces.PutResult;
import dev.wakey.spaces.Space;
import dev.wakey.spaces.SpaceAccessDeniedException;
import dev.wakey.spaces.SpaceNotFoundException;
import dev.wakey.spaces.SpacePrincipal;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceService;
import dev.wakey.spaces.SpaceSlug;
import dev.wakey.spaces.SpaceView;
import dev.wakey.spaces.UserSessions;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

@Slf4j
@RequiredArgsConstructor
final class JdbcSpaceService implements SpaceService {

    private static final int MAX_NAME_LENGTH = 100;

    private static final RowMapper<Space> SPACE_MAPPER = (rs, unusedRowNum) -> new Space(
            rs.getObject("id", UUID.class), rs.getString("slug"), rs.getString("name"), rs.getLong("version"));

    private static final String FIND_SPACE_SQL = """
            SELECT id, slug, name, version
            FROM space
            WHERE slug = :slug
            """;

    private static final String VIEW_SPACE_SQL = """
            SELECT s.id, s.slug, s.name, s.version, m.role
            FROM space s
            LEFT JOIN space_member m ON m.space_id = s.id AND m.subject = :subject
            WHERE s.slug = :slug
            """;

    private static final String INSERT_SPACE_SQL = """
            INSERT INTO space (id, slug, name, version, created_at, updated_at)
            VALUES (:id, :slug, :name, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

    private static final String RENAME_SPACE_SQL = """
            UPDATE space
            SET name = :name, version = version + 1, updated_at = CURRENT_TIMESTAMP
            WHERE slug = :slug
            """;

    private static final String LIST_ALL_SPACES_SQL = """
            SELECT s.id, s.slug, s.name, s.version, m.role
            FROM space s
            LEFT JOIN space_member m ON m.space_id = s.id AND m.subject = :subject
            WHERE s.slug > :afterSlug
            ORDER BY s.slug
            LIMIT :rowLimit
            """;

    private static final String LIST_MEMBER_SPACES_SQL = """
            SELECT s.id, s.slug, s.name, s.version, m.role
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

    private static final String INSERT_MEMBER_SQL = """
            INSERT INTO space_member (space_id, subject, role)
            VALUES (:spaceId, :subject, :role)
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

    @Override
    public PutResult put(SpacePrincipal actor, String slug, String name) {
        requireAdmin(actor);
        String checkedSlug = SpaceSlug.parse(slug).value();
        String checkedName = requireValidName(name);
        Optional<Space> existing = findSpace(checkedSlug);
        return existing.map(space -> keepOrRename(space, checkedName))
                .orElseGet(() -> createOrUpdate(checkedSlug, checkedName));
    }

    @Override
    public Space get(SpacePrincipal actor, String slug) {
        String checkedSlug = SpaceSlug.parse(slug).value();
        Space space = findRequired(checkedSlug);
        boolean visible = actor.admin() || isMember(space.id(), actor.subject());
        if (!visible) {
            throw new SpaceNotFoundException(checkedSlug);
        }
        return space;
    }

    @Override
    public SpaceView view(SpacePrincipal actor, String slug) {
        String checkedSlug = SpaceSlug.parse(slug).value();
        RowMapper<SpaceView> mapper = viewMapper(actor.admin());
        Optional<SpaceView> found = jdbc.sql(VIEW_SPACE_SQL)
                .param("subject", actor.subject())
                .param("slug", checkedSlug)
                .query(mapper)
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
        try {
            jdbc.sql(INSERT_MEMBER_SQL)
                    .param("spaceId", space.id())
                    .param("subject", subject)
                    .param("role", role.name())
                    .update();
        } catch (DuplicateKeyException exception) {
            log.debug(
                    "Subject {} is already a member of space {}; the role is unchanged",
                    subject,
                    space.slug(),
                    exception);
        }
    }

    @Override
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

    private PutResult createOrUpdate(String slug, String name) {
        Optional<Space> created = createIfAbsent(slug, name);
        return created.map(space -> new PutResult(space, true)).orElseGet(() -> keepOrRename(findRequired(slug), name));
    }

    private Optional<Space> createIfAbsent(String slug, String name) {
        try {
            jdbc.sql(INSERT_SPACE_SQL)
                    .param("id", UUID.randomUUID())
                    .param("slug", slug)
                    .param("name", name)
                    .update();
        } catch (DuplicateKeyException exception) {
            log.debug("Space {} was created concurrently; the existing row wins", slug, exception);
            return Optional.empty();
        }
        return findSpace(slug);
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
        String checkedSlug = SpaceSlug.parse(slug).value();
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
        RowMapper<SpaceView> mapper = viewMapper(actor.admin());
        return jdbc.sql(sql)
                .param("subject", actor.subject())
                .param("afterSlug", cursor)
                .param("rowLimit", rowLimit)
                .query(mapper)
                .list();
    }

    private RowMapper<SpaceView> viewMapper(boolean admin) {
        return (rs, unusedRowNum) -> {
            String roleValue = rs.getString("role");
            @Nullable SpaceRole role = roleOrNull(roleValue);
            return new SpaceView(
                    rs.getObject("id", UUID.class),
                    rs.getString("slug"),
                    rs.getString("name"),
                    rs.getLong("version"),
                    admin,
                    role);
        };
    }

    private Optional<Space> findSpace(String slug) {
        return jdbc.sql(FIND_SPACE_SQL).param("slug", slug).query(SPACE_MAPPER).optional();
    }

    private Space findRequired(String slug) {
        return findSpace(slug).orElseThrow(() -> new SpaceNotFoundException(slug));
    }

    private boolean isMember(UUID spaceId, String subject) {
        return findRole(spaceId, subject).isPresent();
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

    private static @Nullable SpaceRole roleOrNull(String value) {
        return value == null ? null : SpaceRole.valueOf(value);
    }
}
