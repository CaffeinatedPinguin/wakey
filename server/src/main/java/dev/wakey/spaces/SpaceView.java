package dev.wakey.spaces;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A space as seen by one caller. An application admin sees every space with {@code admin} set, and
 * {@code role} carries a membership role only when that admin is also a member of the space.
 * {@code planName} is null when the space uses the instance default plan.
 */
public record SpaceView(
        UUID id,
        String slug,
        String name,
        @Nullable String planName,
        long version,
        boolean admin,
        @Nullable SpaceRole role) {}
