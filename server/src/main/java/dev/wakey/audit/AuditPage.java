package dev.wakey.audit;

import org.jspecify.annotations.Nullable;

/** One page request for the history: resume after a cursor and cap the number of entries. */
public record AuditPage(@Nullable AuditCursor after, int limit) {}
