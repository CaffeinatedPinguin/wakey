package dev.wakey.audit;

import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * One successful API write to append to the audit log. The details are a plain map that the audit
 * log serializes once, so callers never build JSON text themselves.
 */
public record AuditChange(
        String action,
        String objectType,
        String objectId,
        @Nullable UUID spaceId,
        AuditActor actor,
        Map<String, ?> details) {}
