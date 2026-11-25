package dev.wakey.audit;

import java.time.Instant;
import java.util.UUID;

/** The sort key of one entry: {@code occurred_at} and {@code id}. */
public record AuditCursor(Instant occurredAt, UUID id) {}
