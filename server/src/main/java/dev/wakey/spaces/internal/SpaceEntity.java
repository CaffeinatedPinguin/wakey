package dev.wakey.spaces.internal;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/** A space row; the version starts at one and grows on every rename. */
@Table("space")
public record SpaceEntity(
        @Id UUID id, String slug, String name, @Version long version, Instant createdAt, Instant updatedAt) {}
