package net.necookie.disastersim.session;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Salted password hashing for the {@code /register}/{@code /login} account system, via
 * {@code PBKDF2WithHmacSHA256} ({@code javax.crypto} — no new Gradle dependency needed).
 *
 * <p>Encoded form: {@code pbkdf2_sha256$<iterations>$<base64 salt>$<base64 hash>} — the iteration
 * count travels with the hash so a future bump doesn't invalidate already-stored passwords.
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int DEFAULT_ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_LENGTH_BITS = 256;
    private static final String PREFIX = "pbkdf2_sha256";

    private PasswordHasher() {}

    /** Hashes {@code password} with a fresh random salt at {@link #DEFAULT_ITERATIONS}. */
    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt, DEFAULT_ITERATIONS);
        return PREFIX + "$" + DEFAULT_ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Verifies {@code password} against a stored {@link #hash} string. Returns {@code false}
     * (never throws) for malformed/foreign-format stored values, so a corrupt row just fails
     * login instead of crashing the command.
     */
    public static boolean verify(String password, String stored) {
        if (stored == null) return false;
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) return false;
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(password.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2 unavailable", e);
        } finally {
            spec.clearPassword();
        }
    }
}
