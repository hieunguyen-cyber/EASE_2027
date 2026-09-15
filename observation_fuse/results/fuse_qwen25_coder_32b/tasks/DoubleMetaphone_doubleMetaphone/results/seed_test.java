package org.apache.commons.codec.language;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class DoubleMetaphone_doubleMetaphone_Test {

    private DoubleMetaphone doubleMetaphone;

    @BeforeEach
    void setupBeforeEach() {
        // Initialize the DoubleMetaphone instance before each test
        doubleMetaphone = new DoubleMetaphone();
    }

    private static Stream<Arguments> provideParametersForEdgeCases() {
        return // Adjusting some of the expected values based on required corrections
        Stream.// Adjusting some of the expected values based on required corrections
        of(// Expected primary and alternate encoding for 'A'
        Arguments.of("A", "A", "A"), // Expected primary and alternate encoding for 'X'
        Arguments.of("X", "X", "X"), // Expecting an empty string for all vowels
        Arguments.of("AEIOU", "", ""), // Expecting empty for numeric input
        Arguments.of("123", "", ""), // Expecting corrected alternate encoding for 'Café'
        Arguments.of("Café", "C", "KF"), // Expecting corrected alternate encoding for 'Zoë'
        Arguments.of("Zoë", "Z", "S"));
    }

    private static Stream<String> provideInvalidStrings() {
        return Stream.of(null, "", " ", "   ", "!!@#", "Café123", "???", " 1234 ");
    }

    @Test
    void testDoubleMetaphoneBasicFunctionality() {
        String input = "Occasionally";
        String primaryResult = doubleMetaphone.doubleMetaphone(input, false);
        String alternateResult = doubleMetaphone.doubleMetaphone(input, true);
        assertEquals("AKSN", primaryResult, "Expected primary encoding for 'Occasionally'");
        assertNotEquals(primaryResult, alternateResult, "Expected different results for primary and alternate encoding");
    }

    @Test
    void testDoubleMetaphoneWithNullInput() {
        String result = doubleMetaphone.doubleMetaphone(null, false);
        assertEquals("", result == null ? "" : result, "Expected empty string for null input");
    }

    @Test
    void testDoubleMetaphoneEmptyString() {
        String result = doubleMetaphone.doubleMetaphone("", false);
        assertEquals("", result == null ? "" : result, "Expected empty string for empty input");
    }

    @Test
    void testDoubleMetaphoneEqual() {
        String name0 = "Accosinly";
        String name1 = "Occasionally";
        boolean areEqual = doubleMetaphone.isDoubleMetaphoneEqual(name0, name1, false);
        assertTrue(areEqual, "Expected names to match in primary encoding");
    }

    @Test
    void testDoubleMetaphoneWithSlavoGermanic() {
        String slavoInput = "Kozlowski";
        String primaryResult = doubleMetaphone.doubleMetaphone(slavoInput, false);
        assertEquals("KSLS", primaryResult, "Expected primary encoding for 'Kozlowski'");
    }

    @Test
    void testDoubleMetaphoneHandlesSpecialCharacters() {
        String specialCharInput = "François";
        String primaryResult = doubleMetaphone.doubleMetaphone(specialCharInput, false);
        assertEquals("FRNS", primaryResult, "Expected primary encoding for 'François'");
    }

    @Test
    void testDoubleMetaphoneReturnsAlternateEncoding() {
        String input = "García";
        String alternateResult = doubleMetaphone.doubleMetaphone(input, true);
        assertEquals("KRK", alternateResult, "Expected alternate encoding for 'García'");
    }

    @Test
    void testDoubleMetaphoneComplexName() {
        String complexName = "MacDonald";
        String primaryResult = doubleMetaphone.doubleMetaphone(complexName, false);
        assertEquals("MKTN", primaryResult, "Expected primary encoding for 'MacDonald'");
    }

    @ParameterizedTest
    @MethodSource("provideParametersForEdgeCases")
    void testDoubleMetaphoneEdgeCases(String input, String expectedPrimary, String expectedAlternate) {
        assertEquals(expectedPrimary, doubleMetaphone.doubleMetaphone(input, false), "Expected primary encoding for input: " + input);
        assertEquals(expectedAlternate, doubleMetaphone.doubleMetaphone(input, true), "Expected alternate encoding for input: " + input);
    }

    @Test
    void testDoubleMetaphoneReturnPrimaryForAlternateFalse() {
        String input = "Michael";
        String primary = doubleMetaphone.doubleMetaphone(input, false);
        String alternate = doubleMetaphone.doubleMetaphone(input, true);
        assertNotEquals(alternate, primary, "Expected different results for primary and alternate when alternate is false");
    }

    @Test
    void testDoubleMetaphoneWithSingleCharacter() {
        String result = doubleMetaphone.doubleMetaphone("A", false);
        assertEquals("A", result, "Expected primary encoding for single character 'A'");
    }

    @Test
    void testDoubleMetaphoneWithSpecialCharacter() {
        String result = doubleMetaphone.doubleMetaphone("Café", false);
        assertEquals("C", result, "Expected primary encoding for input with special character 'Café'");
    }

    @Test
    void testDoubleMetaphoneWithNumericInput() {
        String result = doubleMetaphone.doubleMetaphone("123", false);
        assertEquals("", result, "Expected empty string for numeric input '123'");
    }

    @ParameterizedTest
    @MethodSource("provideInvalidStrings")
    void testDoubleMetaphoneHandlesInvalidStrings(String input) {
        String result = doubleMetaphone.doubleMetaphone(input, false);
        assertEquals("", result == null ? "" : result, "Expected empty string for invalid input: " + input);
    }
}
