package dev.wakey.audit;

import org.jspecify.annotations.Nullable;

/** The actor that caused an event; a system actor has no subject. */
public record AuditActor(@Nullable String subject, ActorKind kind) {}
