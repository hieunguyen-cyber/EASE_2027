package com.google.gson.stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.StringReader;
import java.io.Reader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.io.EOFException;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class JsonReader_nextNonWhitespace_Test {

    private JsonReader jsonReader;

    @BeforeEach
    void setupBeforeEach() {
        // Initialize JsonReader with an empty Reader before each test.
        jsonReader = new JsonReader(new StringReader(""));
    }

    @AfterEach
    void teardownAfterEach() {
        // Retrieve and clean up the JsonReader after each test
        try {
            jsonReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int invokeNextNonWhitespace(boolean throwOnEof) throws Exception {
        Method method = JsonReader.class.getDeclaredMethod("nextNonWhitespace", boolean.class);
        method.setAccessible(true);
        return (int) method.invoke(jsonReader, throwOnEof);
    }

    @Test
    void testNextNonWhitespaceThrowsEOFException() {
        // Testing with an empty input
        jsonReader = new JsonReader(new StringReader(""));
        assertThrows(EOFException.class, () -> invokeNextNonWhitespace(true));
    }

    @Test
    void testNextNonWhitespaceHandlesSpaces() throws Exception {
        // Testing input that contains only spaces
        jsonReader = new JsonReader(new StringReader("   { }"));
        int result = invokeNextNonWhitespace(true);
        // Expects to skip spaces and return '{'
        assertEquals('{', result);
    }

    @Test
    void testNextNonWhitespaceHandlesNewlines() throws Exception {
        // Testing input that starts with newlines
        jsonReader = new JsonReader(new StringReader("\n\n{\"key\": \"value\"}"));
        // skip whitespace
        invokeNextNonWhitespace(true);
        int result = invokeNextNonWhitespace(true);
        // Expects to skip newlines and return '{'
        assertEquals('{', result);
    }

    @Test
    void testNextNonWhitespaceHandlesComments() throws Exception {
        // Testing input that contains a single line comment
        jsonReader = new JsonReader(new StringReader("// comment\n{"));
        // Should skip the comment
        invokeNextNonWhitespace(true);
        int result = invokeNextNonWhitespace(true);
        // Expects to skip comment and return '{'
        assertEquals('{', result);
    }

    @Test
    void testNextNonWhitespaceHandlesCStyleComments() throws Exception {
        // Testing input that contains C-style comments
        jsonReader = new JsonReader(new StringReader("/* comment */ { }"));
        // Set lenient mode
        jsonReader.setLenient(true);
        int result = invokeNextNonWhitespace(true);
        // Expects to skip C-style comments and return '{'
        assertEquals('{', result);
    }

    @Test
    void testNextNonWhitespaceHandlesHashComments() throws Exception {
        // Testing input with hash comments
        jsonReader = new JsonReader(new StringReader("# this is a comment\n{ }"));
        // Set lenient mode
        jsonReader.setLenient(true);
        int result = invokeNextNonWhitespace(true);
        // Expects to skip hash comments and return '{'
        assertEquals('{', result);
    }

    @Test
    void testNextNonWhitespaceHandlesSingleLineComments() throws Exception {
        // Testing input with single line comments
        jsonReader = new JsonReader(new StringReader("// comment\n{"));
        // Skip comment
        invokeNextNonWhitespace(true);
        int result = invokeNextNonWhitespace(true);
        // Expects to skip comment and return '{'
        assertEquals('{', result);
    }

    @Test
    void testNextNonWhitespaceHandlesMultiLineComments() throws Exception {
        // Testing input with multi-line comments
        jsonReader = new JsonReader(new StringReader("/* comment */ { }"));
        // Set lenient mode
        jsonReader.setLenient(true);
        int result = invokeNextNonWhitespace(true);
        // Expects to skip C-style comments and return '{'
        assertEquals('{', result);
    }

    @Test
    void testNextNonWhitespaceHandlesMixedWhitespaceAndComments() throws Exception {
        // Testing input with mixed whitespace and comments
        jsonReader = new JsonReader(new StringReader("  // comment\n\t/* comment */  { }"));
        // Set lenient mode
        jsonReader.setLenient(true);
        int result = invokeNextNonWhitespace(true);
        // Expects to skip mixed whitespace and comments, and return '{'
        assertEquals('{', result);
    }

    @Test
    void testNextNonWhitespaceHandlesThrowOnEOFFalse() throws Exception {
        // Testing with EOF but not throwing an exception
        jsonReader = new JsonReader(new StringReader(""));
        int result = invokeNextNonWhitespace(false);
        // Expects to return -1 when EOF is reached and not throwing an exception
        assertEquals(-1, result);
    }

    @Test
    void testNextNonWhitespaceThrowsSyntaxErrorForUnterminatedComment() throws Exception {
        // Testing for unterminated comment error
        jsonReader = new JsonReader(new StringReader("/* unterminated comment"));
        assertThrows(IOException.class, () -> invokeNextNonWhitespace(true));
    }

    @Test
    void testNextNonWhitespaceThrowsNumberFormatExceptionForMalformedUnicode() throws Exception {
        // Testing for malformed unicode escape sequences
        jsonReader = new JsonReader(new StringReader("\"value with unicode: \\uXYZ\""));
        assertThrows(NumberFormatException.class, () -> invokeNextNonWhitespace(true));
    }
}
