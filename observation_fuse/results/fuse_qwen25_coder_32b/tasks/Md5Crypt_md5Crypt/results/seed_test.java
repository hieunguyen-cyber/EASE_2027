package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import java.security.SecureRandom;
import java.util.Random;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.provider.ArgumentsSource;
// Import Stream for ArgumentsProvider
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
// Added import for ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class Md5Crypt_md5Crypt_Test {

    private byte[] keyBytes;

    private String salt;

    private String prefix;

    private Random random;

    @Nested
    class EdgeCaseTests {

        @Test
        void testMd5CryptWithZeroLengthSalt() {
            assertThrows(IllegalArgumentException.class, () -> {
                Md5Crypt.md5Crypt(keyBytes, "", prefix, random);
            }, "An empty salt string should throw an IllegalArgumentException");
        }

        @Test
        void testMd5CryptWithVeryLongKey() {
            byte[] veryLongKey = new byte[1024];
            for (int i = 0; i < veryLongKey.length; i++) {
                veryLongKey[i] = 'a';
            }
            String result = Md5Crypt.md5Crypt(veryLongKey, salt, prefix, random);
            assertNotNull(result, "Result should not be null for a very long key");
        }
    }

    class NullAndEmptyKeyProvider implements ArgumentsProvider {

        @Override
        public Stream<Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(Arguments.of((byte[]) null), Arguments.of(new byte[0]));
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        keyBytes = "secret".getBytes();
        salt = "$1$foo";
        prefix = "$1$";
        random = new SecureRandom();
    }

    @Test
    void testMd5CryptWithValidInput() {
        String result = Md5Crypt.md5Crypt(keyBytes, salt, prefix, random);
        assertNotNull(result, "Result should not be null for valid input");
        assertTrue(result.matches("^\\$1\\$[a-zA-Z0-9./]{0,8}\\$.{1,}$"), "Result must match the expected format");
    }

    @Test
    void testMd5CryptWithNullSalt() {
        String result = Md5Crypt.md5Crypt(keyBytes, null, prefix, random);
        assertNotNull(result, "Result should not be null when salt is null");
        assertTrue(result.matches("^\\$1\\$[a-zA-Z0-9./]{0,8}\\$.{1,}$"), "Result must match the expected format when salt is generated");
    }

    @Test
    void testMd5CryptWithEmptyKey() {
        String result = Md5Crypt.md5Crypt(new byte[0], salt, prefix, random);
        assertEquals("$1$foo$9mS5ExwgIECGE5YKlD5o91", result, "Expected output does not match for empty key");
    }

    @Test
    void testMd5CryptWithInvalidSalt() {
        assertThrows(IllegalArgumentException.class, () -> {
            Md5Crypt.md5Crypt(keyBytes, "invalid$salt", prefix, random);
        }, "Invalid salt should throw an IllegalArgumentException");
    }

    @Test
    void testMd5CryptWithLongInput() {
        String longInput = "12345678901234567890";
        String result = Md5Crypt.md5Crypt(longInput.getBytes(), salt, prefix, random);
        assertEquals("$1$1234$MoxekaNNUgfPRVqoeYjCD/", result, "Expected output does not match for long input");
    }

    // Using reflection to access the private method if needed
    @Test
    void testMd5CryptWithEdgeCases() throws Exception {
        // Additional edge cases can be included here
    }

    @ParameterizedTest
    @ArgumentsSource(NullAndEmptyKeyProvider.class)
    void testMd5CryptWithNullAndEmptyKey(byte[] key) {
        String result = Md5Crypt.md5Crypt(key, salt, prefix, random);
        assertNotNull(result, "Result should not be null for null or empty key");
    }

    // Using reflection to access the private method if needed
    @Test
    void testMd5CryptWithReflection() throws Exception {
        var method = Md5Crypt.class.getDeclaredMethod("md5Crypt", byte[].class, String.class, String.class, Random.class);
        method.setAccessible(true);
        assertThrows(IllegalArgumentException.class, () -> {
            method.invoke(null, keyBytes, salt, prefix, random);
        });
    }

    @Test
    void testMd5CryptWithInvalidAlgorithm() throws Exception {
        String invalidAlgorithm = "invalidAlgorithm";
        assertThrows(IllegalArgumentException.class, () -> {
            Md5Crypt.md5Crypt(keyBytes, invalidAlgorithm, prefix, random);
        }, "Using an invalid algorithm should throw an IllegalArgumentException");
    }
}
