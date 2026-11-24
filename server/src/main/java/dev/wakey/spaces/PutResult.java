package dev.wakey.spaces;

/** The outcome of an idempotent put: the stored space and whether this call created it. */
public record PutResult(Space space, boolean created) {}
