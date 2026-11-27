package dev.wakey.spaces;

import dev.wakey.spaces.internal.SpaceStore;
import dev.wakey.web.ApiException;
import dev.wakey.web.CommonError;
import dev.wakey.web.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * The membership use cases. The rule that says who may manage a space stays in {@link SpaceService}
 * through {@link SpaceService#viewForWrite}; this class owns membership changes and publishes the
 * events that remove the affected sessions and tokens in the same transaction.
 */
@Validated
@Service
@RequiredArgsConstructor
public class SpaceMembers {

    private final SpaceService spaces;

    private final SpaceStore store;

    private final ApplicationEventPublisher events;

    /**
     * Adds a member to a space the caller manages and reports whether it was created; an existing
     * membership keeps its role.
     */
    public AddMemberResult addMember(CurrentUser actor, @InvalidSlug String slug, @Valid SpaceMember member) {
        SpaceView space = spaces.viewForWrite(actor, slug);
        return store.addMember(space.id(), member);
    }

    /** Lists the members of a space the caller can read, ordered by issuer and subject. */
    public List<SpaceMember> listMembers(CurrentUser actor, @InvalidSlug String slug, SpaceMemberId after, int limit) {
        SpaceView space = spaces.view(actor, slug);
        return store.listMembers(space.id(), after, limit);
    }

    /** Changes a member's role and ends that user's sessions when the role really changes. */
    @Transactional
    public void changeRole(CurrentUser actor, @InvalidSlug String slug, @Valid SpaceMember member) {
        SpaceView space = spaces.viewForWrite(actor, slug);
        SpaceMemberId target = new SpaceMemberId(member.issuer(), member.subject());
        Optional<SpaceRole> current = store.findRole(space.id(), target);
        SpaceRole currentRole = current.orElseThrow(() -> new ApiException(CommonError.NOT_FOUND));
        if (currentRole != member.role()) {
            store.updateRole(space.id(), member);
            UUID spaceId = space.id();
            String issuer = member.issuer();
            String subject = member.subject();
            events.publishEvent(new SpaceMemberRoleChanged(spaceId, issuer, subject));
        }
    }

    /** Removes a member and publishes the event that ends that user's sessions and tokens. */
    @Transactional
    public void removeMember(CurrentUser actor, @InvalidSlug String slug, @Valid SpaceMemberId member) {
        SpaceView space = spaces.viewForWrite(actor, slug);
        if (!store.removeMember(space.id(), member)) {
            throw new ApiException(CommonError.NOT_FOUND);
        }
        UUID spaceId = space.id();
        String issuer = member.issuer();
        String subject = member.subject();
        events.publishEvent(new SpaceMemberRemoved(spaceId, issuer, subject));
    }
}
