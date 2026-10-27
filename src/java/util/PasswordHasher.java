package util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {

    private static final int ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    /** Định dạng theo NT-30: số vòng lặp:salt Base64:hash Base64. */
    public static String hash(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty.");
        }
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derived = derive(password, salt, ITERATIONS);
        Base64.Encoder encoder = Base64.getEncoder();
        return ITERATIONS + ":" + encoder.encodeToString(salt) + ":" + encoder.encodeToString(derived);
    }

    public static boolean verify(String password, String storedHash) {
        if (password == null || storedHash == null || storedHash.length() > 255) {
            return false;
        }
        try {
            // Đọc cả định dạng trong tài liệu khung để dùng chung với phần đăng nhập.
            String value = storedHash.startsWith("pbkdf2_sha256$")
                    ? storedHash.substring("pbkdf2_sha256$".length()).replace('$', ':') : storedHash;
            String[] parts = value.split(":", -1);
            if (parts.length != 3) {
                return false;
            }
            int iterations = Integer.parseInt(parts[0]);
            if (iterations < 1 || iterations > 2_000_000) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            if (salt.length < SALT_BYTES || salt.length > 64 || expected.length != HASH_BYTES) {
                return false;
            }
            return MessageDigest.isEqual(expected, derive(password, salt, iterations));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        char[] characters = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(characters, salt, iterations, HASH_BYTES * 8);
        Arrays.fill(characters, '\0');
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("PBKDF2WithHmacSHA256 is unavailable.", ex);
        } finally {
            spec.clearPassword();
        }
    }
}
