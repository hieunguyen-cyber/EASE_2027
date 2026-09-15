package com.google.gson.stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.io.IOException;
import java.io.StringReader;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class JsonReader_readEscapeCharacter_Test {

    private JsonReader jsonReader;

    private StringReader stringReader;

    @BeforeEach
    void setupBeforeEach() {
        // Initialize required dependencies before each test
        stringReader = new StringReader("");
        jsonReader = new JsonReader(stringReader);
    }

    @AfterEach
    void teardownAfterEach() {
        // Cleanup resources after each test
        // Uncommented to close the JsonReader after tests
//         jsonReader.close();
    }

    private int extractLineNumber(JsonReader jsonReader) {
        // Since lineNumber is private, use reflection to access it safely
        try {
            Field lineNumberField = JsonReader.class.getDeclaredField("lineNumber");
            lineNumberField.setAccessible(true);
            return (int) lineNumberField.get(jsonReader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testReadEscapeCharacter_validEscapeSequences() throws Exception {
        stringReader = new StringReader("\"Hello\\nWorld\"");
        jsonReader = new JsonReader(stringReader);
        // Read the string value directly
        String value = jsonReader.nextString();
        // Verify that the string contains a newline
        assertTrue(value.contains("\n"));
    }

    @Test
    void testReadEscapeCharacter_invalidEscapeSequence() {
        stringReader = new StringReader("\"Hello\\zWorld\"");
        jsonReader = new JsonReader(stringReader);
        assertThrows(MalformedJsonException.class, () -> {
            jsonReader.nextString();
        });
    }

    @Test
    void testReadEscapeCharacter_unterminatedEscapeSequence() {
        stringReader = new StringReader("\"Hello\\u\"");
        jsonReader = new JsonReader(stringReader);
        assertThrows(MalformedJsonException.class, () -> {
            jsonReader.nextString();
        });
    }

    @Test
    void testReadEscapeCharacter_validUnicodeEscape() throws Exception {
        stringReader = new StringReader("\"Hello\\u0041\"");
        jsonReader = new JsonReader(stringReader);
        String value = jsonReader.nextString();
        // Verify that we get the expected string
        assertEquals("HelloA", value);
    }

    @Test
    void testReadEscapeCharacter_nextLineChange() throws Exception {
        stringReader = new StringReader("\"Hello\\nWorld\"");
        jsonReader = new JsonReader(stringReader);
        String value = jsonReader.nextString();
        // Ensure new line was properly read
        assertTrue(value.contains("\n"));
        // Verify line number increased (line 0 after the string)
        assertEquals(1, extractLineNumber(jsonReader));
    }

    @Test
    void testReadEscapeCharacter_validTabEscape() throws Exception {
        stringReader = new StringReader("\"Hello\\tWorld\"");
        jsonReader = new JsonReader(stringReader);
        String value = jsonReader.nextString();
        // Ensure tab character was read
        assertTrue(value.contains("\t"));
    }

    @Test
    void testReadEscapeCharacter_backslashEscape() throws Exception {
        stringReader = new StringReader("\"Hello\\\\World\"");
        jsonReader = new JsonReader(stringReader);
        String value = jsonReader.nextString();
        // Ensure backslash character was read
        assertTrue(value.contains("\\"));
    }

    @Test
    void testReadEscapeCharacter_validSingleQuoteEscape() throws Exception {
        stringReader = new StringReader("\"Hello\\'World\"");
        jsonReader = new JsonReader(stringReader);
        String value = jsonReader.nextString();
        // Ensure single quote character was read
        assertTrue(value.contains("'"));
    }
}