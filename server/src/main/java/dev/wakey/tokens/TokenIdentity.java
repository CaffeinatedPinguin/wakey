package dev.wakey.tokens;

import java.util.UUID;

/** An authenticated token with the space it belongs to and the scope it may really use right now. */
public record TokenIdentity(UUID tokenId, UUID spaceId, String spaceSlug, String ownerSubject, TokenScope scope) {}
