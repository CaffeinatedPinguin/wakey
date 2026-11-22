package dev.wakey.leadership;

import java.time.OffsetDateTime;

public record LeaseGrant(String name, String holderId, long fencingToken, OffsetDateTime expiresAt) {}
