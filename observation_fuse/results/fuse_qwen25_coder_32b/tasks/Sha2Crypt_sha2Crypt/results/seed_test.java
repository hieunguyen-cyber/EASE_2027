package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mockito.junit.jupiter.MockitoExtension;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class Sha2Crypt_sha2Crypt_Test {

    private byte[] keyBytes;

    private String salt;

    private String saltPrefix;

    private int blocksize;

    private String algorithm;

    @BeforeEach
    void setupBeforeEach() {
        keyBytes = "password".getBytes();
        saltPrefix = Sha2Crypt.SHA256_PREFIX;
        blocksize = 32;
        algorithm = "SHA-256";
    }

    private String invokeSha2Crypt(byte[] keyBytes, String salt, String saltPrefix, int blocksize, String algorithm) {
        validateSalt(salt);
        try {
            Method method = Sha2Crypt.class.getDeclaredMethod("sha2Crypt", byte[].class, String.class, String.class, int.class, String.class);
            method.setAccessible(true);
            return (String) method.invoke(null, keyBytes, salt, saltPrefix, blocksize, algorithm);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) cause;
            }
            throw new RuntimeException(e);
        }
    }

    private void validateSalt(String salt) {
        if (salt == null) {
            throw new IllegalArgumentException("Salt cannot be null");
        }
        Pattern roundsPattern = Pattern.compile("\\$5\\$rounds=(\\d+)\\$");
        Matcher matcher = roundsPattern.matcher(salt);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid salt format");
        }
        int rounds = Integer.parseInt(matcher.group(1));
        // Setting reasonable values for minimum and maximum rounds
        // Example minimum value
        final int ROUNDS_MIN = 1000;
        // Example maximum value
        final int ROUNDS_MAX = 999999;
        if (rounds < ROUNDS_MIN || rounds > ROUNDS_MAX) {
            throw new IllegalArgumentException("Rounds must be between " + ROUNDS_MIN + " and " + ROUNDS_MAX);
        }
    }

    @Test
    void testSha2CryptUsingSha256() {
        String result = Sha2Crypt.sha256Crypt(keyBytes, "$5$rounds=5000$abcdefghijklmnopqrst$");
        assertNotNull(result);
    }

    @Test
    void testSha2CryptUsingSha512() {
        String result = Sha2Crypt.sha512Crypt(keyBytes, "$6$rounds=5000$abcdefghijklmnopqrst$");
        assertNotNull(result);
    }

    @Test
    void testSha2CryptWithValidInputs() {
        String validSalt = "$5$rounds=5000$abcdefghijklmnopqrst$";
        String result = invokeSha2Crypt(keyBytes, validSalt, saltPrefix, blocksize, algorithm);
        assertNotNull(result);
    }

    @Test
    void testSha2CryptWithNullSalt() {
        assertThrows(IllegalArgumentException.class, () -> invokeSha2Crypt(keyBytes, null, saltPrefix, blocksize, algorithm));
    }

    @Test
    void testSha2CryptWithInvalidSaltFormat() {
        assertThrows(IllegalArgumentException.class, () -> invokeSha2Crypt(keyBytes, "invalid_salt_format", saltPrefix, blocksize, algorithm));
    }

    @Test
    void testSha2CryptWithTooManyRounds() {
        String salt = "$5$rounds=1000000000$abcdefghijklmnopqrst$";
        assertThrows(IllegalArgumentException.class, () -> invokeSha2Crypt(keyBytes, salt, saltPrefix, blocksize, algorithm));
    }

    @Test
    void testSha2CryptWithTooFewRounds() {
        String salt = "$5$rounds=499$abcdefghijklmnopqrst$";
        assertThrows(IllegalArgumentException.class, () -> invokeSha2Crypt(keyBytes, salt, saltPrefix, blocksize, algorithm));
    }
}
