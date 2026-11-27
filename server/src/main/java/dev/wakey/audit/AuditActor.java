package dev.wakey.audit;

import org.jspecify.annotations.Nullable;

/** The actor that caused an event; a system actor has no subject. */
public record AuditActor(@Nullable String subject, ActorKind kind) {

    /** Creates the actor of an API write from the subject and whether an API token authenticated it. */
    public static AuditActor of(String subject, boolean token) {
        return new AuditActor(subject, token ? ActorKind.TOKEN : ActorKind.USER);
    }
}
