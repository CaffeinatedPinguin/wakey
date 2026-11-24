package dev.wakey.spaces;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A space as seen by one caller. An application admin sees every space with {@code admin} set, and
 * {@code role} carries a membership role only when that admin is also a member of the space.
 */
public record SpaceView(
        UUID id,
        String slug,
        String name,
        long version,
        boolean admin,
        @Nullable SpaceRole role) {}
