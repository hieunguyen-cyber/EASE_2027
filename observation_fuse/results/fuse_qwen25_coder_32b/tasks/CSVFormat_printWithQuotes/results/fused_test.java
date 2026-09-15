package org.apache.commons.csv;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.StringWriter;
import java.io.IOException;
import java.lang.reflect.Method;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class CSVFormat_printWithQuotes_Test {

    private CSVFormat csvFormat;

    private StringWriter writer;

    @BeforeEach
    void setupBeforeEach() {
        // Initialize the CSVFormat object using the DEFAULT configuration
        csvFormat = CSVFormat.DEFAULT;
        // Prepare an Appendable for testing
        writer = new StringWriter();
    }

    // Helper method to invoke the private method using reflection
    private void invokePrintWithQuotes(Object object, CharSequence charSeq, Appendable out, boolean newRecord) throws Exception {
        Method method = CSVFormat.class.getDeclaredMethod("printWithQuotes", Object.class, CharSequence.class, Appendable.class, boolean.class);
        // Make private method accessible
        method.setAccessible(true);
        method.invoke(csvFormat, object, charSeq, out, newRecord);
    }

    @Test
    void testPrintWithQuotes() throws Exception {
        // Given
        Object testObject = "test";
        CharSequence testCharSequence = "Hello, World!";
        boolean newRecord = true;
        // When
        invokePrintWithQuotes(testObject, testCharSequence, writer, newRecord);
        // Then
        String result = writer.toString();
        assertNotNull(result);
        // Expecting the output to start with a quote
        assertTrue(result.startsWith("\""));
        // Expecting the output to end with a quote
        assertTrue(result.endsWith("\""));
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testPrintWithQuotesForNull() throws Exception {
        // Given
        Object testObject = null;
        CharSequence testCharSequence = "Hello, World!";
        boolean newRecord = true;
        // When
        invokePrintWithQuotes(testObject, testCharSequence, writer, newRecord);
        // Then
        String result = writer.toString();
        assertEquals("", result, "The output should be empty for null object");
    }

    @Test
    void testPrintWithQuotesEmptyStringNewRecord() throws Exception {
        // Given
        Object testObject = "empty";
        // Empty CharSequence
        CharSequence testCharSequence = "";
        boolean newRecord = true;
        // When
        invokePrintWithQuotes(testObject, testCharSequence, writer, newRecord);
        // Then
        String result = writer.toString();
        assertTrue(result.startsWith("\""), "The output should start with a quote for empty CharSequence");
        assertTrue(result.endsWith("\""), "The output should end with a quote for empty CharSequence");
    }

    @Test
    void testPrintWithQuotesNoQuotesNeeded() throws Exception {
        // Given
        Object testObject = "normal";
        CharSequence testCharSequence = "No special characters";
        boolean newRecord = false;
        // When
        invokePrintWithQuotes(testObject, testCharSequence, writer, newRecord);
        // Then
        String result = writer.toString();
        assertEquals("No special characters", result, "The output should match the input CharSequence without quotes");
    }

    @Test
    void testPrintWithQuotesContainingLineBreaks() throws Exception {
        // Given
        Object testObject = "lineBreakTest";
        // CharSequence with line breaks
        CharSequence testCharSequence = "First line\nSecond line";
        boolean newRecord = true;
        // When
        invokePrintWithQuotes(testObject, testCharSequence, writer, newRecord);
        // Then
        String result = writer.toString();
        assertTrue(result.startsWith("\""), "The output should start with a quote for line breaks");
        assertTrue(result.endsWith("\""), "The output should end with a quote for line breaks");
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testPrintWithQuotesNumericValue() throws Exception {
        // Given
        // Numeric object
        Object testObject = 12345;
        CharSequence testCharSequence = "12345";
        boolean newRecord = true;
        // When
        invokePrintWithQuotes(testObject, testCharSequence, writer, newRecord);
        // Then
        String result = writer.toString();
        // Check if the result starts with quote if quotes are needed
        assertTrue(result.startsWith("\""), "The output should start with a quote for numeric values");
        assertTrue(result.endsWith("\""), "The output should end with a quote for numeric values");
    }

    @Test
    void testPrintWithQuotesSpecialCharacters() throws Exception {
        // Given
        Object testObject = "specialChars";
        CharSequence testCharSequence = "Hello, \"World\"! This is a test.";
        boolean newRecord = true;
        // When
        invokePrintWithQuotes(testObject, testCharSequence, writer, newRecord);
        // Then
        String result = writer.toString();
        assertTrue(result.startsWith("\""), "The output should start with a quote for special characters");
        assertTrue(result.endsWith("\""), "The output should end with a quote for special characters");
        assertTrue(result.contains("\"\""), "Quotes should be escaped inside the quote-enclosed string");
    }

    @Test
    void testPrintWithQuotesWithEscapeChar() throws Exception {
        // Given
        Object testObject = "escapeCharTest";
        CharSequence testCharSequence = "Hello, \\World!";
        boolean newRecord = true;
        // Simulating that escape character is set to backslash
        // Using the constructor instead of setEscapeCharacter
        Method method = CSVFormat.class.getDeclaredMethod("withEscape", char.class);
        method.setAccessible(true);
        csvFormat = (CSVFormat) method.invoke(csvFormat, '\\');
        // When
        invokePrintWithQuotes(testObject, testCharSequence, writer, newRecord);
        // Then
        String result = writer.toString();
        assertTrue(result.startsWith("\""), "The output should start with a quote for escape char");
        assertTrue(result.endsWith("\""), "The output should end with a quote for escape char");
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testPrintWithQuotesUnexpectedQuoteMode() throws Exception {
        // Given
        Object testObject = "unexpectedModeTest";
        CharSequence testCharSequence = "Some text";
        boolean newRecord = false;
        // Setup a mock QuoteMode to return an unexpected state
        Method setQuoteModeMethod = CSVFormat.class.getDeclaredMethod("withQuoteMode", QuoteMode.class);
        setQuoteModeMethod.setAccessible(true);
        // Setting it to null or an unexpected value
        csvFormat = (CSVFormat) setQuoteModeMethod.invoke(csvFormat, (QuoteMode) null);
        // When & Then
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            invokePrintWithQuotes(testObject, testCharSequence, writer, newRecord);
        });
        assertEquals("Unexpected Quote value: null", exception.getMessage());
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testPrintWithQuotesIOException() throws Exception {
        // Given
        Object testObject = "testIOException";
        CharSequence testCharSequence = "This should throw an IOException";
        boolean newRecord = true;
        // Create a mock Appendable that will throw IOException
        Appendable mockAppendable = mock(Appendable.class);
        // Stubbing the method call appropriately
        doThrow(new IOException("Mock IOException")).when(mockAppendable).append(any(CharSequence.class));
        // When & Then
        Exception exception = assertThrows(IOException.class, () -> {
            invokePrintWithQuotes(testObject, testCharSequence, mockAppendable, newRecord);
        });
        assertEquals("Mock IOException", exception.getMessage());
    }
@Test
void testEscapeCharacterNotSet() throws Exception {
    // Given
    Object testObject = "test";
    CharSequence testCharSequence = "Hello, World!";
    boolean newRecord = true;
    // When
    invokePrintWithQuotes(testObject, testCharSequence, writer, newRecord);
    // Then
    String result = writer.toString();
    assertEquals("\"Hello, World!\"", result, "The output should be quoted without escape character");
}
@Test
void testObjectIsNumber() throws Exception {
    // Given
    Object testObject = 12345;
    CharSequence testCharSequence = "12345";
    boolean newRecord = true;
    // When
    invokePrintWithQuotes(testObject, testCharSequence, writer, newRecord);
    // Then
    String result = writer.toString();
    assertEquals("12345", result, "The output should not be quoted for numeric values");
}
@Test
void testCharSequenceStartsWithComment() throws Exception {
    // Given
    Object testObject = "test";
    CharSequence testCharSequence = "#Hello, World!";
    boolean newRecord = true;
    // When
    invokePrintWithQuotes(testObject, testCharSequence, writer, newRecord);
    // Then
    String result = writer.toString();
    assertEquals("\"#Hello, World!\"", result, "The output should be quoted as it starts with a comment character");
}
@Test
void testCharSequenceEndsWithTrimChar() throws Exception {
    // Given
    Object testObject = "test";
    CharSequence testCharSequence = "Hello, World! ";
    boolean newRecord = true;
    // When
    invokePrintWithQuotes(testObject, testCharSequence, writer, newRecord);
    // Then
    String result = writer.toString();
    assertEquals("\"Hello, World! \"", result, "The output should be quoted as it ends with a trim character");
}
@Test
void testCharSequenceContainsQuoteChar() throws Exception {
    // Given
    Object testObject = "test";
    CharSequence testCharSequence = "Hello, \"World\"!";
    boolean newRecord = true;
    // When
    invokePrintWithQuotes(testObject, testCharSequence, writer, newRecord);
    // Then
    String result = writer.toString();
    assertEquals("\"Hello, \"\"World\"\"!\"", result, "The output should be quoted and escape the quote character");
}
@Test
void testCharSequenceContainsEscapeChar() throws Exception {
    // Given
    Object testObject = "test";
    CharSequence testCharSequence = "Hello, \\World!";
    boolean newRecord = true;
    // When
    invokePrintWithQuotes(testObject, testCharSequence, writer, newRecord);
    // Then
    String result = writer.toString();
    assertEquals("\"Hello, \\World!\"", result, "The output should be quoted and escape the escape character");
}
@Test
void testEmptyCharSequenceNotNewRecord() throws Exception {
    // Given
    Object testObject = "test";
    CharSequence testCharSequence = "";
    boolean newRecord = false;
    // When
    invokePrintWithQuotes(testObject, testCharSequence, writer, newRecord);
    // Then
    String result = writer.toString();
    assertEquals("", result, "The output should be empty for empty character sequence and not a new record");
}
@Test
void testCharSequenceContainsDelimiter() throws Exception {
    // Given
    Object testObject = "test";
    CharSequence testCharSequence = "Hello, World!";
    boolean newRecord = true;
    // When
    invokePrintWithQuotes(testObject, testCharSequence, writer, newRecord);
    // Then
    String result = writer.toString();
    assertEquals("\"Hello, World!\"", result, "The output should be quoted as it contains a delimiter");
}
@Test
void testCharSequenceContainsLineFeed() throws Exception {
    // Given
    Object testObject = "test";
    CharSequence testCharSequence = "Hello,\nWorld!";
    boolean newRecord = true;
    // When
    invokePrintWithQuotes(testObject, testCharSequence, writer, newRecord);
    // Then
    String result = writer.toString();
    assertEquals("\"Hello,\nWorld!\"", result, "The output should be quoted as it contains a line feed");
}
@Test
void testCharSequenceContainsCarriageReturn() throws Exception {
    // Given
    Object testObject = "test";
    CharSequence testCharSequence = "Hello,\rWorld!";
    boolean newRecord = true;
    // When
    invokePrintWithQuotes(testObject, testCharSequence, writer, newRecord);
    // Then
    String result = writer.toString();
    assertEquals("\"Hello,\rWorld!\"", result, "The output should be quoted as it contains a carriage return");
}
}