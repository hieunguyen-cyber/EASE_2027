package com.google.gson.stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.StringReader;
import java.io.Reader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.io.EOFException;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class JsonReader_nextUnquotedValue_Test {

    private JsonReader jsonReader;

    @BeforeEach
    void setupBeforeEach() {
        jsonReader = new JsonReader(new StringReader("test input for testing nextUnquotedValue()"));
        // Set lenient mode to allow for unquoted value handling in malformed JSON
        jsonReader.setLenient(true);
    }

    @AfterEach
    void teardownAfterEach() {
        try {
            jsonReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Utility method to invoke the private method using reflection
    private String invokeNextUnquotedValue() throws Exception {
        Method method = JsonReader.class.getDeclaredMethod("nextUnquotedValue");
        method.setAccessible(true);
        return (String) method.invoke(jsonReader);
    }

    @Test
    void testNextUnquotedValue() throws Exception {
        jsonReader = new JsonReader(new StringReader("expected value"));
        String value = invokeNextUnquotedValue();
        assertEquals("expected value", value);
    }

    @Test
    void testNextUnquotedValueWithoutSpecialCharacters() throws Exception {
        String input = "hello world";
        jsonReader = new JsonReader(new StringReader(input));
        jsonReader.setLenient(true);
        String value = invokeNextUnquotedValue();
        assertEquals("hello world", value);
    }

    @Test
    void testNextUnquotedValueWithSpecialCharacter() throws Exception {
        String input = "foo;bar";
        jsonReader = new JsonReader(new StringReader(input));
        jsonReader.setLenient(true);
        String value = invokeNextUnquotedValue();
        // Adjust according to your expected result logic
        assertEquals("foo", value);
    }

    @Test
    void testNextUnquotedValueWithWhitespace() throws Exception {
        String input = "   testValue  ";
        jsonReader = new JsonReader(new StringReader(input));
        jsonReader.setLenient(true);
        String value = invokeNextUnquotedValue();
        assertEquals("testValue", value);
    }

    @Test
    void testNextUnquotedValueAtEndOfInput() throws Exception {
        String input = "finalValue";
        jsonReader = new JsonReader(new StringReader(input));
        jsonReader.setLenient(true);
        String value = invokeNextUnquotedValue();
        assertEquals("finalValue", value);
    }

    @Test
    void testNextUnquotedValueWithMultipleValues() throws Exception {
        String input = "value1, value2, value3";
        jsonReader = new JsonReader(new StringReader(input));
        jsonReader.setLenient(true);
        String firstValue = invokeNextUnquotedValue();
        assertEquals("value1", firstValue);
        // Create a new reader to read the next value
        // Simulating the next call
        String secondInput = " value2, value3";
        jsonReader = new JsonReader(new StringReader(secondInput));
        jsonReader.setLenient(true);
        String secondValue = invokeNextUnquotedValue();
        assertEquals("value2", secondValue);
    }

    @Test
    void testNextUnquotedValueEmptyInput() throws Exception {
        String input = "";
        jsonReader = new JsonReader(new StringReader(input));
        jsonReader.setLenient(true);
        // expect empty string for empty input
        assertEquals("", invokeNextUnquotedValue());
    }

    @Test
    void testNextUnquotedValueEOFException() throws Exception {
        String input = "value1, ";
        jsonReader = new JsonReader(new StringReader(input));
        jsonReader.setLenient(true);
        invokeNextUnquotedValue();
        jsonReader = new JsonReader(new StringReader(""));
        assertThrows(EOFException.class, this::invokeNextUnquotedValue);
    }
}
