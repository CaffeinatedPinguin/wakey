package dev.wakey.tokens;

/** The member who owns a token; the role is read from the spaces module on every call. */
public record TokenOwner(String subject) {}
