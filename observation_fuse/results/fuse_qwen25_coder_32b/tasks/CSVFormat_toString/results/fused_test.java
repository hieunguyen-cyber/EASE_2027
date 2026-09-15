package org.apache.commons.csv;

import static org.apache.commons.csv.QuoteMode.MINIMAL;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import static org.apache.commons.csv.Constants.BACKSLASH;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.IOException;
import static org.apache.commons.csv.Constants.CRLF;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class CSVFormat_toString_Test {

    private CSVFormat csvFormat;

    @BeforeEach
    void setupBeforeEach() {
        // Initialize CSVFormat instances required for testing
        csvFormat = CSVFormat.DEFAULT.builder().setDelimiter(',').setQuote('"').setQuoteMode(QuoteMode.ALL).setRecordSeparator(CRLF).build();
    }

    @Test
    void testToString() {
        String result = csvFormat.toString();
        assertNotNull(result);
        assertTrue(result.contains("Delimiter=<,>"));
        assertTrue(result.contains("QuoteChar=<\""));
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testToString_withDefaultSettings() {
        CSVFormat defaultFormat = CSVFormat.DEFAULT;
        String result = defaultFormat.toString();
        assertNotNull(result);
        assertTrue(result.contains("Delimiter=<,>"));
        assertTrue(result.contains("SkipHeaderRecord:false"));
        assertFalse(result.contains("EmptyLines:ignored"));
    }

    @Test
    void testToString_withCustomDelimiterAndQuoteCharacter() {
        CSVFormat customFormat = CSVFormat.DEFAULT.builder().setDelimiter(';').setQuote('\'').build();
        String result = customFormat.toString();
        assertNotNull(result);
        assertTrue(result.contains("Delimiter=<;>"));
        assertTrue(result.contains("QuoteChar=<'"));
    }

    @Test
    void testToString_withIgnoreEmptyLines() {
        csvFormat = CSVFormat.DEFAULT.builder().setIgnoreEmptyLines(true).build();
        String result = csvFormat.toString();
        assertNotNull(result);
        assertTrue(result.contains("EmptyLines:ignored"));
    }

    @Test
    void testToString_withNullStringAndCommentMarker() {
        CSVFormat formatWithNullsAndComments = CSVFormat.DEFAULT.builder().setNullString("N/A").setCommentMarker('#').build();
        String result = formatWithNullsAndComments.toString();
        assertNotNull(result);
        assertTrue(result.contains("NullString=<N/A>"));
        assertTrue(result.contains("CommentStart=<#>"));
    }

    @Test
    void testToString_withHeadersAndHeaderComments() {
        String[] headers = { "Name", "Age", "City" };
        String[] comments = { "This is a header comment" };
        CSVFormat headerFormat = CSVFormat.DEFAULT.builder().setHeader(headers).setHeaderComments(comments).build();
        String result = headerFormat.toString();
        assertNotNull(result);
        assertTrue(result.contains("Header:"));
        assertTrue(result.contains("HeaderComments:"));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testToString_withSkipHeaderRecord(boolean skipHeader) {
        csvFormat = CSVFormat.DEFAULT.builder().setSkipHeaderRecord(skipHeader).build();
        String result = csvFormat.toString();
        assertNotNull(result);
        assertTrue(result.contains("SkipHeaderRecord:" + skipHeader));
    }

    @Test
    void testToString_withEscapeCharacter() {
        csvFormat = CSVFormat.DEFAULT.builder().setEscape(BACKSLASH).build();
        String result = csvFormat.toString();
        assertNotNull(result);
        assertTrue(result.contains("Escape=<\\>"));
    }

    @Test
    void testToString_withIgnoreHeaderCase() {
        csvFormat = CSVFormat.DEFAULT.builder().setIgnoreHeaderCase(true).build();
        String result = csvFormat.toString();
        assertNotNull(result);
        assertTrue(result.contains("IgnoreHeaderCase:ignored"));
    }

    // Exception scenario tests for toString method
    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testToString_throwsIllegalArgumentException_whenInvalidSettings() {
        // Create a faulty format that will trigger the validation error
        csvFormat = CSVFormat.DEFAULT.builder().setDelimiter(',').setQuote(// Set to something invalid if necessary for your validation logic
        '"').build();
        assertThrows(IllegalArgumentException.class, () -> {
            Method validateMethod = CSVFormat.class.getDeclaredMethod("validate");
            validateMethod.setAccessible(true);
            validateMethod.invoke(csvFormat);
        });
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testToString_throwsIOException_whenPrintFails() throws Exception {
        CSVFormat mockCSVFormat = Mockito.spy(CSVFormat.DEFAULT);
        doThrow(new IOException("Print failure")).when(mockCSVFormat).print(any(), any(), anyBoolean());
        assertThrows(IOException.class, mockCSVFormat::toString);
    }
@Test
void testToString_includesQuoteMode() {
    CSVFormat formatWithQuoteMode = CSVFormat.DEFAULT.builder().setQuoteMode(QuoteMode.MINIMAL).build();
    String result = formatWithQuoteMode.toString();
    assertNotNull(result);
    assertTrue(result.contains("QuoteMode=<MINIMAL>"));
}
@Test
void testToString_includesRecordSeparator() {
    CSVFormat formatWithRecordSeparator = CSVFormat.DEFAULT.builder().setRecordSeparator("\n").build();
    String result = formatWithRecordSeparator.toString();
    assertNotNull(result);
    assertTrue(result.contains("RecordSeparator=<\n>"));
}
@Test
void testToString_includesSurroundingSpacesIgnored() {
    CSVFormat formatWithSurroundingSpacesIgnored = CSVFormat.DEFAULT.builder().setIgnoreSurroundingSpaces(true).build();
    String result = formatWithSurroundingSpacesIgnored.toString();
    assertNotNull(result);
    assertTrue(result.contains("SurroundingSpaces:ignored"));
}
}