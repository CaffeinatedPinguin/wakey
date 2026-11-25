package dev.wakey.spaces.internal;

import dev.wakey.spaces.SpaceRole;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

/** A membership row keyed by the space and the member subject. */
@Table("space_member")
public record SpaceMemberEntity(
        @Id @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY) SpaceMemberKey key, SpaceRole role) {}
