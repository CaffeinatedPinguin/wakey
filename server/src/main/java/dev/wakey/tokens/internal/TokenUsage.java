package dev.wakey.tokens.internal;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** A last-used write that touches only {@code last_used_at}, so it cannot clear a concurrent revoke. */
@Table("api_token")
public record TokenUsage(@Id UUID id, Instant lastUsedAt) {}
