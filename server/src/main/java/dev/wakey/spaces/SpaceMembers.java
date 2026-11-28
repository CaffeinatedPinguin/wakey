package dev.wakey.spaces;

import dev.wakey.spaces.internal.SpaceAudit;
import dev.wakey.spaces.internal.SpaceStore;
import dev.wakey.web.ApiException;
import dev.wakey.web.CommonError;
import dev.wakey.web.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * The membership use cases. The rule that says who may manage a space stays in {@link SpaceService}
 * through {@link SpaceService#viewForWrite}; this class owns membership changes and publishes the
 * events that remove the affected sessions and tokens in the same transaction.
 */
@Slf4j
@Validated
@Service
@RequiredArgsConstructor
public class SpaceMembers {

    private final SpaceService spaces;

    private final SpaceStore store;

    private final ApplicationEventPublisher events;

    private final SpaceAudit audit;

    /**
     * Adds a member to a space the caller manages and reports whether it was created; an existing
     * membership keeps its role.
     */
    @Transactional
    public AddMemberResult addMember(
            CurrentUser actor, @InvalidSlug String slug, @Valid SpaceMember member, boolean viaToken) {
        var space = spaces.viewForWrite(actor, slug);
        AddMemberResult result;
        try {
            result = store.addMember(space.id(), member);
        } catch (DuplicateKeyException exception) {
            log.debug("Membership of subject {} already exists", member.subject(), exception);
            SpaceMemberId key = new SpaceMemberId(member.issuer(), member.subject());
            SpaceRole role = store.findRole(space.id(), key).orElseThrow();
            SpaceMember stored = new SpaceMember(member.issuer(), member.subject(), role);
            result = new AddMemberResult(stored, false);
        }
        SpaceMember stored = result.member();
        if (result.created()) {
            var auditActor = SpaceAudit.actor(actor.subject(), viaToken);
            audit.memberAdded(auditActor, space.id(), stored);
        }
        log.info(
                "Member {} (issuer {}) in space {} has role {} (created: {}) by {} (admin: {})",
                stored.subject(),
                stored.issuer(),
                space.id(),
                stored.role(),
                result.created(),
                actor.subject(),
                actor.admin());
        return result;
    }

    /** Lists the members of a space the caller can read, ordered by issuer and subject. */
    public List<SpaceMember> listMembers(CurrentUser actor, @InvalidSlug String slug, SpaceMemberId after, int limit) {
        var space = spaces.view(actor, slug);
        var members = store.listMembers(space.id(), after, limit);
        log.debug("Listed {} members in space {} with limit {}", members.size(), space.id(), limit);
        return members;
    }

    /** Changes a member's role and ends that user's sessions when the role really changes. */
    @Transactional
    public void changeRole(CurrentUser actor, @InvalidSlug String slug, @Valid SpaceMember member, boolean viaToken) {
        var space = spaces.viewForWrite(actor, slug);
        SpaceMemberId target = new SpaceMemberId(member.issuer(), member.subject());
        var current = store.findRole(space.id(), target);
        SpaceRole currentRole = current.orElseThrow(() -> new ApiException(CommonError.NOT_FOUND));
        if (currentRole != member.role()) {
            store.updateRole(space.id(), member);
            var spaceId = space.id();
            String issuer = member.issuer();
            String subject = member.subject();
            events.publishEvent(new SpaceMemberRoleChanged(spaceId, issuer, subject));
            var auditActor = SpaceAudit.actor(actor.subject(), viaToken);
            audit.memberRoleChanged(auditActor, spaceId, member, currentRole);
            log.info(
                    "Changed role of member {} (issuer {}) in space {} from {} to {} by {} (admin: {})",
                    subject,
                    issuer,
                    spaceId,
                    currentRole,
                    member.role(),
                    actor.subject(),
                    actor.admin());
        }
    }

    /** Removes a member and publishes the event that ends that user's sessions and tokens. */
    @Transactional
    public void removeMember(
            CurrentUser actor, @InvalidSlug String slug, @Valid SpaceMemberId member, boolean viaToken) {
        var space = spaces.viewForWrite(actor, slug);
        if (!store.removeMember(space.id(), member)) {
            throw new ApiException(CommonError.NOT_FOUND);
        }
        var spaceId = space.id();
        String issuer = member.issuer();
        String subject = member.subject();
        events.publishEvent(new SpaceMemberRemoved(spaceId, issuer, subject));
        var auditActor = SpaceAudit.actor(actor.subject(), viaToken);
        audit.memberRemoved(auditActor, spaceId, member);
        log.info(
                "Removed member {} (issuer {}) from space {} by {} (admin: {})",
                subject,
                issuer,
                spaceId,
                actor.subject(),
                actor.admin());
    }
}
