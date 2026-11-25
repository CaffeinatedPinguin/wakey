package dev.wakey.tokens;

/** Rejects a token field that does not follow the domain rules; the HTTP layer maps it to 400. */
public final class InvalidTokenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String field;

    private final String code;

    public InvalidTokenException(String field, String code, String message) {
        super(message);
        this.field = field;
        this.code = code;
    }

    public String field() {
        return field;
    }

    public String code() {
        return code;
    }
}
