package dev.wakey.spaces;

import java.util.UUID;

/** A space as stored; the version grows whenever the name changes. */
public record Space(UUID id, String slug, String name, long version) {}
