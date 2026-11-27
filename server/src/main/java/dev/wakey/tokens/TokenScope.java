package dev.wakey.tokens;

/** The scope a token asks for; the effective scope is the smaller of it and the owner's role. */
public enum TokenScope {
    READ,
    WRITE
}
