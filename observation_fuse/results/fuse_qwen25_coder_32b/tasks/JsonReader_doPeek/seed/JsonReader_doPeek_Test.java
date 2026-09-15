package com.google.gson.stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.StringReader;
import java.io.EOFException;
import java.lang.reflect.InvocationTargetException;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class JsonReader_doPeek_Test {

    private JsonReader jsonReader;

    @BeforeEach
    void setupBeforeEach() {
        jsonReader = new JsonReader(new StringReader("{\"key\":\"value\"}"));
    }

    @AfterEach
    void teardownAfterEach() {
        try {
            if (jsonReader != null) {
                jsonReader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Helper method to invoke the private doPeek method via reflection
    private int invokeDoPeek() throws IOException {
        try {
            var method = JsonReader.class.getDeclaredMethod("doPeek");
            method.setAccessible(true);
            return (int) method.invoke(jsonReader);
        } catch (InvocationTargetException e) {
            // Handle the exception thrown from the doPeek method
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            } else {
                throw new IOException("Error invoking doPeek", cause);
            }
        } catch (Exception e) {
            throw new IOException("Error invoking doPeek", e);
        }
    }

    @Test
    void testDoPeekUnexpectedValue() throws IOException {
        // This should throw a syntax error
        jsonReader = new JsonReader(new StringReader("[1, ,2]"));
        jsonReader.beginArray();
        assertThrows(IOException.class, this::invokeDoPeek);
        jsonReader.endArray();
    }

    @Test
    void testDoPeekHandlesEOF() throws IOException {
        jsonReader = new JsonReader(new StringReader(""));
        assertThrows(EOFException.class, this::invokeDoPeek);
    }

    @Test
    void testDoPeekWhenClosed() throws IOException {
        jsonReader.close();
        assertThrows(IllegalStateException.class, this::invokeDoPeek);
    }

    @Test
    void testDoPeekUnexpectedValueThrowsSyntaxError() throws IOException {
        // Malformed JSON
        jsonReader = new JsonReader(new StringReader("[1, 2, , 3]"));
        jsonReader.beginArray();
        assertThrows(IOException.class, this::invokeDoPeek);
        jsonReader.endArray();
    }

    @Test
    void testDoPeekOnEmptyDocument() throws IOException {
        jsonReader = new JsonReader(new StringReader(""));
        assertThrows(EOFException.class, this::invokeDoPeek);
    }

    @Test
    void testDoPeekWithMalformedJsonThrowsException() throws IOException {
        // Incomplete JSON
        jsonReader = new JsonReader(new StringReader("{"));
        assertThrows(IOException.class, this::invokeDoPeek);
    }

    @Test
    void testDoPeekOnMalformedEscapeCharacter() throws IOException {
        // Malformed escape character
        jsonReader = new JsonReader(new StringReader("{\"key\":\"bad\\uGA\"}"));
        jsonReader.beginObject();
        assertThrows(IOException.class, this::invokeDoPeek);
        jsonReader.endObject();
    }
}
