package dev.wakey.tokens.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Generates and hashes token secrets; a secret never leaves this class except in the create result. */
final class TokenSecrets {

    static final String PREFIX = "wk_";

    private static final int SECRET_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final HexFormat HEX = HexFormat.of();

    private TokenSecrets() {}

    static String generate() {
        byte[] randomBytes = new byte[SECRET_BYTES];
        RANDOM.nextBytes(randomBytes);
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return PREFIX + encoded;
    }

    static String hash(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
            byte[] hashed = digest.digest(bytes);
            return HEX.formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
