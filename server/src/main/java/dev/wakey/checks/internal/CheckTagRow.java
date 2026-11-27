package dev.wakey.checks.internal;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

/** A tag row read by the tag filter; the aggregate saves tags through its own mapped collection. */
@Table("check_tag")
public record CheckTagRow(
        @Id @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY) CheckTagKey key, UUID spaceId) {}
