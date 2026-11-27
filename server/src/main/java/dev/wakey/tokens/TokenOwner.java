package dev.wakey.tokens;

/** The member who owns a token: the OIDC issuer and subject pair; the role is read from the spaces module. */
public record TokenOwner(String issuer, String subject) {}
