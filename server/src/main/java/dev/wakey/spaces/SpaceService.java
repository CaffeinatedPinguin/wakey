package dev.wakey.spaces;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** The module's public port; the HTTP layer maps the domain exceptions to Problem Details. */
public interface SpaceService {

    /**
     * Creates a space or renames it by slug. Only an application admin may call it, and a repeat
     * with the same name changes nothing while a different name bumps the version.
     */
    PutResult put(SpacePrincipal actor, String slug, String name);

    /** Reads a space the caller can see; admins see every space, members see their own, others get 404. */
    Space get(SpacePrincipal actor, String slug);

    /**
     * Lists the spaces the caller can see, ordered by slug and resuming after the given slug.
     * Admins see every space; members see only the spaces they belong to.
     */
    List<SpaceView> listVisible(SpacePrincipal actor, @Nullable String afterSlug, int limit);

    /** Adds a member to a space the caller manages; an existing membership keeps its role. */
    void addMember(SpacePrincipal actor, String slug, String subject, SpaceRole role);

    /** Changes a member's role and invalidates that user's sessions when the role really changes. */
    void changeRole(SpacePrincipal actor, String slug, String subject, SpaceRole role);

    /** Removes a member and invalidates that user's sessions. */
    void removeMember(SpacePrincipal actor, String slug, String subject);
}
