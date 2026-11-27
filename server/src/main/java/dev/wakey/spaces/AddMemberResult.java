package dev.wakey.spaces;

/** The outcome of adding a member: the stored member and whether this call created it. */
public record AddMemberResult(SpaceMember member, boolean created) {}
