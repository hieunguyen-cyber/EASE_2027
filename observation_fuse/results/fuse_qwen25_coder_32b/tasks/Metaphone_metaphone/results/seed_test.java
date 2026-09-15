package org.apache.commons.codec.language;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class Metaphone_metaphone_Test {

    private Metaphone metaphone;

    @BeforeEach
    void setupBeforeEach() {
        // Initialize the Metaphone instance before each test
        metaphone = new Metaphone();
    }

    static Stream<Arguments> provideStringsForMetaphoneTesting() {
        return Stream.of(
            // Special vowel case
            Arguments.of("AE", "A"), // Check regular case
            Arguments.of("BAUGH", "B"), // Silent 'B' at the end
            Arguments.of("SILENCE", "S"), // PH -> F
            Arguments.of("BOMB", "B"),
            // Silent H after G
            Arguments.of("PHYSICAL", "F")
        );
    }

    // Reflection helper to test private methods if required
    private String invokePrivateMetaphone(String input) throws Exception {
        Method method = Metaphone.class.getDeclaredMethod("metaphone", String.class);
        method.setAccessible(true);
        return (String) method.invoke(metaphone, input);
    }

    @Test
    void testMetaphone() {
        assertEquals("SNS", metaphone.metaphone("SCIENCE"));
        assertEquals("SN", metaphone.metaphone("SCENE"));
        assertEquals("S", metaphone.metaphone("SCY"));
    }

    @Test
    void testSilentGN() {
        // Check handling of silent 'G's in words like "GNU" and "SIGNED"
        assertEquals("N", metaphone.metaphone("GNU"));
        assertEquals("SNT", metaphone.metaphone("SIGNED"));
    }

    @Test
    void testSilentHAfterG() {
        // Validate that 'H' after 'G' is silent in words like "GHENT" and "BAUGH"
        assertEquals("KNT", metaphone.metaphone("GHENT"));
        assertEquals("B", metaphone.metaphone("BAUGH"));
    }

    @Test
    void testMaxCodeLength() {
        // Ensure that the generated metaphone code does not exceed the maximum length set
        metaphone.setMaxCodeLen(4);
        // Should trim to 'AKSK'
        assertEquals("AKSK", metaphone.metaphone("AXEAXE"));
    }

    @Test
    void testMetaphoneEqual() {
        assertTrue(metaphone.isMetaphoneEqual("Case", "case"));
        assertTrue(metaphone.isMetaphoneEqual("CASE", "Case"));
        assertTrue(metaphone.isMetaphoneEqual("caSe", "cAsE"));
        assertTrue(metaphone.isMetaphoneEqual("quick", "cookie"));
    }

    @Test
    void testEmptyString() {
        // Check the response for an empty string input
        assertEquals("", metaphone.metaphone(""));
    }

    @Test
    void testNullInput() {
        // Test the behavior when null is passed as input
        assertEquals("", metaphone.metaphone(null));
    }

    @Test
    void testMetaphoneWithScientificWords() {
        // Test for words related to science to assert proper phonetic representation
        assertEquals("SNS", metaphone.metaphone("SCIENCE"));
        assertEquals("SN", metaphone.metaphone("SCENE"));
        assertEquals("S", metaphone.metaphone("SCY"));
    }

    @Test
    void testMetaphoneWithSpecialCases() {
        // Testing cases with special handling such as initial 'K', 'G', and 'W'
        // 'W' + 'R' -> 'R'
        assertEquals("R", metaphone.metaphone("WRITER"));
        // 'W' + 'H' -> 'W'
        assertEquals("W", metaphone.metaphone("WHATEVER"));
        // 'K' + 'N' -> 'N'
        assertEquals("K", metaphone.metaphone("KNOT"));
    }

    @Test
    void testMetaphoneEqualCases() {
        // Test isMetaphoneEqual for various scenarios
        assertTrue(metaphone.isMetaphoneEqual("Case", "case"));
        assertTrue(metaphone.isMetaphoneEqual("CASE", "Case"));
        assertTrue(metaphone.isMetaphoneEqual("caSe", "cAsE"));
        assertTrue(metaphone.isMetaphoneEqual("quick", "cookie"));
    }

    @ParameterizedTest
    @MethodSource("provideStringsForMetaphoneTesting")
    void testMetaphoneParameterized(String input, String expected) {
        assertEquals(expected, metaphone.metaphone(input));
    }

    // New test methods to handle potential exceptions
    @Test
    void testMetaphoneWithInvalidCharacters() {
        // Since the method expects only A-Z uppercase characters, we should handle invalid characters.
        // This case should not throw an exception, but we can check the response is as expected
        // when invalid characters are included.
        // Non-alpha characters
        assertSame("", metaphone.metaphone("123"));
        // Special characters
        assertSame("", metaphone.metaphone("!@#$%^&*()"));
    }

    @Test
    void testMaxLengthExceeded() {
        // Check that metaphone code length is capped if it exceeds a certain length
        metaphone.setMaxCodeLen(1);
        // Should only return first character
        assertEquals("A", metaphone.metaphone("ALPHA"));
    }

    @Test
    void testMultipleConsequentSameCharacters() {
        // Test for inputs with multiple same characters
        // Check for handling multiple 'A's
        assertEquals("A", metaphone.metaphone("AA"));
        // Consecutive 'K's should only return one 'K'
        assertEquals("K", metaphone.metaphone("KKK"));
    }
}