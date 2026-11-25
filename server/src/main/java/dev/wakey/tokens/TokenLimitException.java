package dev.wakey.tokens;

/** Rejects creating a token when the owner already has the maximum number of active tokens. */
public final class TokenLimitException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int bound;

    public TokenLimitException(int bound) {
        super("The owner has reached the token limit of " + bound);
        this.bound = bound;
    }

    public int bound() {
        return bound;
    }
}
