package dev.wakey.spaces.internal;

import dev.wakey.audit.AuditActor;
import dev.wakey.audit.AuditChange;
import dev.wakey.audit.AuditLog;
import dev.wakey.spaces.SpaceMember;
import dev.wakey.spaces.SpaceMemberId;
import dev.wakey.spaces.SpaceRole;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Appends the audit entries of the membership writes in the caller's transaction. */
@Component
@RequiredArgsConstructor
public class SpaceAudit {

    private static final String MEMBER = "space_member";

    private final AuditLog auditLog;

    /** The actor of one membership write, resolved from the caller's subject and its token provenance. */
    public static AuditActor actor(String subject, boolean viaToken) {
        return AuditActor.of(subject, viaToken);
    }

    public void memberAdded(AuditActor actor, UUID spaceId, SpaceMember member) {
        Map<String, Object> details = memberDetails(member.issuer(), member.subject(), member.role());
        Target target = new Target("space_member.add", spaceId.toString(), spaceId);
        append(target, actor, details);
    }

    public void memberRoleChanged(AuditActor actor, UUID spaceId, SpaceMember member, SpaceRole oldRole) {
        String newRole = member.role().name();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("issuer", member.issuer());
        details.put("subject", member.subject());
        details.put("oldRole", oldRole.name());
        details.put("newRole", newRole);
        Target target = new Target("space_member.role", spaceId.toString(), spaceId);
        append(target, actor, details);
    }

    public void memberRemoved(AuditActor actor, UUID spaceId, SpaceMemberId member) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("issuer", member.issuer());
        details.put("subject", member.subject());
        Target target = new Target("space_member.remove", spaceId.toString(), spaceId);
        append(target, actor, details);
    }

    private void append(Target target, AuditActor actor, Map<String, Object> details) {
        AuditChange change =
                new AuditChange(target.action(), MEMBER, target.objectId(), target.spaceId(), actor, details);
        auditLog.record(change);
    }

    private static Map<String, Object> memberDetails(String issuer, String subject, SpaceRole role) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("issuer", issuer);
        details.put("subject", subject);
        details.put("role", role.name());
        return details;
    }

    private record Target(String action, String objectId, UUID spaceId) {}
}
