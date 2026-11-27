package dev.wakey.tokens;

import java.util.UUID;

/** An authenticated token with its space, its owner identity and the scope it may really use right now. */
public record TokenIdentity(
        UUID tokenId, UUID spaceId, String spaceSlug, String ownerIssuer, String ownerSubject, TokenScope scope) {}
