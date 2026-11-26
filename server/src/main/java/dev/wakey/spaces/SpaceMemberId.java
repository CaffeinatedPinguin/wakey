package dev.wakey.spaces;

/** The OIDC issuer and subject pair that identifies a member, without the role. */
public record SpaceMemberId(
        @InvalidIssuer String issuer, @InvalidSubject String subject) {}
