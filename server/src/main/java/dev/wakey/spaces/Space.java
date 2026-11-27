package dev.wakey.spaces;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** A space as stored; the version grows whenever the name or the plan changes. */
public record Space(
        UUID id, String slug, String name, @Nullable String planName, long version) {}
