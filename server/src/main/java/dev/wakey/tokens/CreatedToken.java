package dev.wakey.tokens;

/** A newly created token; the secret appears here only and is redacted from {@link #toString()}. */
public record CreatedToken(String secret, TokenSummary token) {

    @Override
    public String toString() {
        return "CreatedToken[token=" + token + ", secret=<redacted>]";
    }
}
