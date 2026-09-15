package org.apache.commons.codec.language;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class DoubleMetaphone_handleS_Test {

    private DoubleMetaphone doubleMetaphone;

    private DoubleMetaphone.DoubleMetaphoneResult result;

    @BeforeEach
    void setupBeforeEach() {
        // Initialize the DoubleMetaphone instance before each test
        doubleMetaphone = new DoubleMetaphone();
        result = doubleMetaphone.new DoubleMetaphoneResult(doubleMetaphone.getMaxCodeLen());
    }

    private int invokeHandleS(final String value, final DoubleMetaphone.DoubleMetaphoneResult result, int index, final boolean slavoGermanic) {
        // Reflection is used here to invoke the private handleS method since it's not accessible directly.
        try {
            var method = DoubleMetaphone.class.getDeclaredMethod("handleS", String.class, DoubleMetaphone.DoubleMetaphoneResult.class, int.class, boolean.class);
            method.setAccessible(true);
            return (int) method.invoke(doubleMetaphone, value, result, index, slavoGermanic);
        } catch (Exception e) {
            fail("Failed to invoke handleS method: " + e.getMessage());
            // Return original index in case of failure
            return index;
        }
    }

    @Test
    void testHandleS_WithSpecialCaseIsland() {
        // Given
        String value = "island";
        // Start at the beginning
        int index = 0;
        boolean slavoGermanic = false;
        // When
        index = invokeHandleS(value, result, index, slavoGermanic);
        // Then
        assertEquals("S", result.getPrimary(), "Primary result should be 'S'");
        assertEquals("", result.getAlternate(), "Alternate result should be empty");
    }

    @Test
    void testHandleS_WithSpecialCaseSugar() {
        // Given
        String value = "sugar";
        int index = 0;
        boolean slavoGermanic = false;
        // When
        index = invokeHandleS(value, result, index, slavoGermanic);
        // Then
        assertEquals("X", result.getPrimary(), "Primary result should be 'X'");
        assertEquals("S", result.getAlternate(), "Alternate result should be 'S'");
    }

    @Test
    void testHandleS_WithSpecialCaseSH() {
        // Given
        String value = "shepherd";
        int index = 0;
        boolean slavoGermanic = false;
        // When
        index = invokeHandleS(value, result, index, slavoGermanic);
        // Then
        assertEquals("X", result.getPrimary(), "Primary result should be 'X' for 'SH'");
        assertEquals("", result.getAlternate(), "Alternate result should be empty");
    }

    @Test
    void testHandleS_WithSpecialCaseSIAN() {
        // Given
        String value = "sian";
        int index = 0;
        boolean slavoGermanic = false;
        // When
        index = invokeHandleS(value, result, index, slavoGermanic);
        // Then
        assertEquals("S", result.getPrimary(), "Primary result should be 'S'");
        assertEquals("X", result.getAlternate(), "Alternate result should be 'X' when slavoGermanic is false");
    }

    @Test
    void testHandleS_WithSlavoGermanicTrueShouldAppendS() {
        // Given
        String value = "sian";
        int index = 0;
        boolean slavoGermanic = true;
        // When
        index = invokeHandleS(value, result, index, slavoGermanic);
        // Then
        assertEquals("S", result.getPrimary(), "Primary result should be 'S' when slavoGermanic is true");
        assertEquals("", result.getAlternate(), "Alternate result should be empty when slavoGermanic is true");
    }

    @Test
    void testHandleS_EndCharacterWithAI_OI() {
        // Given
        String value = "artois";
        // Start at the last character
        int index = value.length() - 1;
        boolean slavoGermanic = false;
        // When
        index = invokeHandleS(value, result, index, slavoGermanic);
        // Then
        assertEquals("S", result.getAlternate(), "Alternate result should have 'S' in this French case");
    }

    @Test
    void testHandleS_WithNothingMatched() {
        // Given
        String value = "test";
        // Start at the beginning
        int index = 0;
        boolean slavoGermanic = false;
        // When
        index = invokeHandleS(value, result, index, slavoGermanic);
        // Then
        assertEquals("S", result.getPrimary(), "Primary result should be 'S'");
        assertEquals("", result.getAlternate(), "Alternate result should be empty");
    }

    @ParameterizedTest
    @CsvSource({ // Testing the last character
    // Testing the last character
    // Testing the last character
    "sugar, 0, false, X, S", // Testing the last character
    "island, 0, false, S, ", "shepherd, 0, false, X, ", "sian, 0, false, S, X", "artois, 5, false, , S", "test, 0, false, S, " })
    void testHandleS_Parameterized(String value, int startIndex, boolean slavoGermanic, String expectedPrimary, String expectedAlternate) {
        // Given
        int index = startIndex;
        // When
        index = invokeHandleS(value, result, index, slavoGermanic);
        // Then
        assertEquals(expectedPrimary, result.getPrimary(), "Primary result should match expected.");
        assertEquals(expectedAlternate, result.getAlternate(), "Alternate result should match expected.");
    }

    @Test
    void testHandleS_NullValue() {
        // Given
        String value = null;
        int index = 0;
        boolean slavoGermanic = false;
        // When
        Exception exception = assertThrows(NullPointerException.class, () -> {
            invokeHandleS(value, result, index, slavoGermanic);
        });
        // Then
        assertEquals("java.lang.NullPointerException: value cannot be null", exception.getMessage());
    }

    @Test
    void testHandleS_NegativeIndex() {
        // Given
        String value = "sugar";
        // Invalid index
        int index = -1;
        boolean slavoGermanic = false;
        // When
        Exception exception = assertThrows(IndexOutOfBoundsException.class, () -> {
            invokeHandleS(value, result, index, slavoGermanic);
        });
        // Then
        assertEquals("java.lang.IndexOutOfBoundsException: Index out of bounds", exception.getMessage());
    }

    @Test
    void testHandleS_IndexGreaterThanLength() {
        // Given
        String value = "island";
        // Index is equal to length
        int index = value.length();
        boolean slavoGermanic = false;
        // When
        Exception exception = assertThrows(IndexOutOfBoundsException.class, () -> {
            invokeHandleS(value, result, index, slavoGermanic);
        });
        // Then
        assertEquals("java.lang.IndexOutOfBoundsException: Index out of bounds", exception.getMessage());
    }

    @Test
    void testHandleS_EmptyString() {
        // Given
        // Empty input case
        String value = "";
        int index = 0;
        boolean slavoGermanic = false;
        // When
        index = invokeHandleS(value, result, index, slavoGermanic);
        // Then
        assertEquals("", result.getPrimary(), "Primary result should be empty for empty input");
        assertEquals("", result.getAlternate(), "Alternate result should also be empty");
    }
}
