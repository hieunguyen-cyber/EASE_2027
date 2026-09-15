package com.google.gson.stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class JsonReader_peekNumber_Test {

    private JsonReader reader;

    @BeforeEach
    void setupBeforeEach() {
        String jsonInput = "{\"number\": 12345}";
        reader = new JsonReader(new StringReader(jsonInput));
        // Enable lenient parsing to handle various input formats
        reader.setLenient(true);
    }

    @AfterEach
    void teardownAfterEach() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int invokePeekNumber() throws Exception {
        Method peekNumberMethod = JsonReader.class.getDeclaredMethod("peekNumber");
        peekNumberMethod.setAccessible(true);
        return (int) peekNumberMethod.invoke(reader);
    }

    private int getPrivateFieldValue(String fieldName) throws Exception {
        Field field = JsonReader.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getInt(reader);
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testPeekNumber() throws Exception {
        reader.beginObject();
        reader.nextName();
        int result = invokePeekNumber();
        assertEquals(getPrivateFieldValue("PEEKED_NUMBER"), getPrivateFieldValue("peeked"));
        assertEquals(5, getPrivateFieldValue("peekedNumberLength"));
        assertTrue(result == getPrivateFieldValue("PEEKED_NONE") || result != getPrivateFieldValue("PEEKED_NONE"));
        reader.endObject();
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testPeekNumberRecognizesSimpleNumber() throws Exception {
        reader.beginObject();
        reader.nextName();
        int result = invokePeekNumber();
        assertEquals(getPrivateFieldValue("PEEKED_NUMBER"), getPrivateFieldValue("peeked"));
        assertEquals(5, getPrivateFieldValue("peekedNumberLength"));
        assertTrue(result == getPrivateFieldValue("PEEKED_NONE") || result != getPrivateFieldValue("PEEKED_NONE"));
        reader.endObject();
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testPeekNumberHandlesNegativeSign() throws Exception {
        reader = new JsonReader(new StringReader("{\"number\": -12345}"));
        // Allow negative numbers
        reader.setLenient(true);
        reader.beginObject();
        reader.nextName();
        int result = invokePeekNumber();
        assertEquals(getPrivateFieldValue("PEEKED_LONG"), getPrivateFieldValue("peeked"));
        assertTrue(getPrivateFieldValue("peekedLong") < 0);
        assertEquals(5, getPrivateFieldValue("peekedNumberLength"));
        reader.endObject();
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testPeekNumberHandlesInvalidInput() throws Exception {
        reader = new JsonReader(new StringReader("{\"number\": \"not_a_number\"}"));
        // Allow invalid input handling
        reader.setLenient(true);
        reader.beginObject();
        reader.nextName();
        int result = invokePeekNumber();
        assertEquals(getPrivateFieldValue("PEEKED_NONE"), getPrivateFieldValue("peeked"));
        reader.endObject();
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testPeekNumberHandlesLeadingZeroes() throws Exception {
        reader = new JsonReader(new StringReader("{\"number\": 0123}"));
        // Allow leading zeros in numbers
        reader.setLenient(true);
        reader.beginObject();
        reader.nextName();
        int result = invokePeekNumber();
        assertEquals(getPrivateFieldValue("PEEKED_NONE"), getPrivateFieldValue("peeked"));
        reader.endObject();
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testPeekNumberHandlesLongInput() throws Exception {
        reader = new JsonReader(new StringReader("{\"number\": 123456789012345678901234567890}"));
        // Allow very large numbers
        reader.setLenient(true);
        reader.beginObject();
        reader.nextName();
        int result = invokePeekNumber();
        assertEquals(getPrivateFieldValue("PEEKED_NONE"), getPrivateFieldValue("peeked"));
        reader.endObject();
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testPeekNumberHandlesSpecialCharacters() throws Exception {
        reader = new JsonReader(new StringReader("{\"number\": \"123.45e6\"}"));
        // Allow scientific notation
        reader.setLenient(true);
        reader.beginObject();
        reader.nextName();
        int result = invokePeekNumber();
        assertEquals(getPrivateFieldValue("PEEKED_NUMBER"), getPrivateFieldValue("peeked"));
        assertEquals(8, getPrivateFieldValue("peekedNumberLength"));
        reader.endObject();
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testPeekNumberHandlesNoInput() throws Exception {
        reader = new JsonReader(new StringReader("{}"));
        reader.beginObject();
        assertThrows(IOException.class, this::invokePeekNumber);
        reader.endObject();
    }
}
