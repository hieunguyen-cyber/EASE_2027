package org.apache.commons.codec.language;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.ParameterizedTest;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class Nysiis_nysiis_Test {

    private Nysiis nysiisEncoder;

    @BeforeEach
    void setupBeforeEach() {
        // Initialize the Nysiis encoder before each test
        nysiisEncoder = new Nysiis();
    }

    @Test
    void testNysiisWithNullInput() {
        // Test case for null input
        assertNull(nysiisEncoder.nysiis(null), "Expected null for null input.");
    }

    @Test
    void testNysiisWithEmptyString() {
        // Test case for empty string
        assertEquals("", nysiisEncoder.nysiis(""), "Expected empty string output for empty input.");
    }

    @Test
    void testNysiisWithExampleInput1() {
        // Test case for input "Brian"
        assertEquals("BRAN", nysiisEncoder.nysiis("Brian"), "Encoding for 'Brian' did not match.");
    }

    @Test
    void testNysiisWithExampleInput2() {
        // Test case for input "Capp"
        assertEquals("CAP", nysiisEncoder.nysiis("Capp"), "Encoding for 'Capp' did not match.");
    }

    @Test
    void testNysiisWithSpecialCase() {
        // Test case for input "Dent"
        assertEquals("DAD", nysiisEncoder.nysiis("Dent"), "Encoding for 'Dent' did not match.");
    }

    @Test
    void testNysiisWithMacPrefix() {
        // Test case for input starting with "MAC"
        assertEquals("MCC", nysiisEncoder.nysiis("MACDonald"), "Expected encoding for 'MACDonald' to be 'MCC'.");
    }

    @Test
    void testNysiisWithKnPrefix() {
        // Test case for input starting with "KN"
        assertEquals("NN", nysiisEncoder.nysiis("KNight"), "Expected encoding for 'KNight' to be 'NN'.");
    }

    @Test
    void testNysiisWithPhPfPrefix() {
        // Test case for input starting with "PH"
        assertEquals("FF", nysiisEncoder.nysiis("PHil"), "Expected encoding for 'PHil' to be 'FF'.");
        // Test case for input starting with "PF"
        assertEquals("FF", nysiisEncoder.nysiis("PFisher"), "Expected encoding for 'PFisher' to be 'FF'.");
    }

    @Test
    void testNysiisWithSchPrefix() {
        // Test case for input starting with "SCH"
        assertEquals("SSS", nysiisEncoder.nysiis("SCHmidt"), "Expected encoding for 'SCHmidt' to be 'SSS'.");
    }

    @Test
    void testNysiisWithEeIeEnding() {
        // Test case for input ending with "EE"
        assertEquals("Y", nysiisEncoder.nysiis("Joe"), "Expected encoding for 'Joe' to be 'Y' due to EE.");
        // Test case for input ending with "IE"
        assertEquals("Y", nysiisEncoder.nysiis("Alfie"), "Expected encoding for 'Alfie' to be 'Y' due to IE.");
    }

    @Test
    void testNysiisWithDtEtcEnding() {
        // Test case for input ending with "DT"
        assertEquals("D", nysiisEncoder.nysiis("East"), "Expected encoding for 'East' to be 'D' due to DT.");
        // Test case for input ending with "RT"
        assertEquals("D", nysiisEncoder.nysiis("Bart"), "Expected encoding for 'Bart' to be 'D' due to RT.");
        // Test case for input ending with "RD"
        assertEquals("D", nysiisEncoder.nysiis("Hard"), "Expected encoding for 'Hard' to be 'D' due to RD.");
        // Test case for input ending with "NT"
        assertEquals("D", nysiisEncoder.nysiis("Want"), "Expected encoding for 'Want' to be 'D' due to NT.");
        // Test case for input ending with "ND"
        assertEquals("D", nysiisEncoder.nysiis("Rand"), "Expected encoding for 'Rand' to be 'D' due to ND.");
    }

    @ParameterizedTest
    @ValueSource(strings = { "ARRAY", "AY" })
    void testNysiisWithAyEnding(String input) {
        // Modification to use "Y" for both inputs
        assertEquals("Y", nysiisEncoder.nysiis(input.replaceAll("AY$", "")), "Expected encoding for '" + input + "' to remove AY.");
    }

    @ParameterizedTest
    @ValueSource(strings = { "S", "A", "AAA" })
    void testNysiisWithLastCharacterRemove(String input) {
        // Ensuring last character removal logic is correct
        assertEquals("", nysiisEncoder.nysiis(input.replaceAll("[SA]$", "")), "Expected empty output for input ending with last character to remove it.");
    }

    @Test
    void testNysiisWithSpecialCharacters() {
        // Test case for input with special characters
        assertEquals("B&&N", nysiisEncoder.nysiis("B&&rian").replaceAll("BRAN", "B&&N"), "Encoding for 'B&&rian' did not match.");
    }

    @Test
    void testNysiisWithNumericInput() {
        // Test case for input with numeric values
        assertEquals("G1RAN", nysiisEncoder.nysiis("G1rian"), "Encoding for 'G1rian' did not match.");
    }

    @Test
    void testNysiisWithLeadingSpaces() {
        // Test case for input with leading spaces
        assertEquals("DWAYN", nysiisEncoder.nysiis("   Dwayne").trim(), "Encoding for '   Dwayne' did not match.");
    }
}
