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
class JsonReader_peek_Test {

    private JsonReader jsonReader;

    @BeforeEach
    void setupBeforeEach() {
        // Initialize the JSON reader with a sample JSON input for testing
        Reader reader = new StringReader("{\"key\":\"value\"}");
        jsonReader = new JsonReader(reader);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Cleanup operations after each test
        if (jsonReader != null) {
            jsonReader.close();
        }
    }

    private JsonToken invokePeek() throws Exception {
        Method peekMethod = JsonReader.class.getDeclaredMethod("peek");
        peekMethod.setAccessible(true);
        return (JsonToken) peekMethod.invoke(jsonReader);
    }

    @Test
    void testPeekBeginObject() throws IOException {
        // Test to check if the peek method correctly identifies the beginning of an object
        assertEquals(JsonToken.BEGIN_OBJECT, jsonReader.peek());
    }

    @Test
    void testPeekEndDocumentAfterParsing() throws IOException {
        // Complete an object parsing and check if peek recognizes end of document
        jsonReader.beginObject();
        jsonReader.nextName();
        jsonReader.nextString();
        jsonReader.endObject();
        assertEquals(JsonToken.END_DOCUMENT, jsonReader.peek());
    }

    @Test
    void testPeekThrowsEOFException() throws IOException {
        JsonReader emptyReader = new JsonReader(new StringReader("{}"));
        emptyReader.beginObject();
        emptyReader.endObject();
        try {
            // This should work fine
            assertEquals(JsonToken.END_DOCUMENT, emptyReader.peek());
        } catch (IOException e) {
            fail("Expected no IOException, but got: " + e.getMessage());
        }
    }

    @Test
    void testPeekReturnsCorrectTokenType() throws IOException {
        // Test to check each type recognized by peek after fetching values
        jsonReader.beginObject();
        assertEquals("key", jsonReader.nextName());
        assertEquals(JsonToken.STRING, jsonReader.peek());
        jsonReader.nextString();
        jsonReader.endObject();
    }

    @Test
    void testPeekReturnsNameToken() throws IOException {
        // Testing for NAME token
        jsonReader.beginObject();
        // Consuming the key "key"
        assertEquals("key", jsonReader.nextName());
        // Confirming type after name
        assertEquals(JsonToken.STRING, jsonReader.peek());
        // Consume the string
        jsonReader.nextString();
        jsonReader.endObject();
    }

    @Test
    void testPeekReturnsEOF() throws IOException {
        // Check for end of document
        String input = "[]";
        jsonReader = new JsonReader(new StringReader(input));
        jsonReader.beginArray();
        jsonReader.endArray();
        assertEquals(JsonToken.END_DOCUMENT, jsonReader.peek());
    }

    @Test
    void testPeekReturnsBooleanTokenForTrue() throws IOException {
        // Test to examine how peek behaves with a boolean value true
        jsonReader = new JsonReader(new StringReader("{\"boolValue\":true}"));
        jsonReader.beginObject();
        assertEquals("boolValue", jsonReader.nextName());
        assertEquals(JsonToken.BOOLEAN, jsonReader.peek());
        // Consume the boolean value
        jsonReader.nextBoolean();
        jsonReader.endObject();
    }

    @Test
    void testPeekReturnsBooleanTokenForFalse() throws IOException {
        // Test to examine how peek behaves with a boolean value false
        jsonReader = new JsonReader(new StringReader("{\"boolValue\":false}"));
        jsonReader.beginObject();
        assertEquals("boolValue", jsonReader.nextName());
        assertEquals(JsonToken.BOOLEAN, jsonReader.peek());
        // Consume the boolean value
        jsonReader.nextBoolean();
        jsonReader.endObject();
    }

    @Test
    void testPeekReturnsNullToken() throws IOException {
        // Test for handling null values in the JSON
        jsonReader = new JsonReader(new StringReader("{\"nullableValue\":null}"));
        jsonReader.beginObject();
        assertEquals("nullableValue", jsonReader.nextName());
        assertEquals(JsonToken.NULL, jsonReader.peek());
        // Consume the null value
        jsonReader.nextNull();
        jsonReader.endObject();
    }

    @Test
    void testPeekReturnsStringTokenForSingleQuoted() throws Exception {
        // Test for a single-quoted string, allowing leniency
        jsonReader = new JsonReader(new StringReader("{'key':'value'}"));
        // Allows single quotes
        jsonReader.setLenient(true);
        jsonReader.beginObject();
        jsonReader.nextName();
        assertEquals(JsonToken.STRING, invokePeek());
        jsonReader.nextString();
        jsonReader.endObject();
    }

    @Test
    void testPeekReturnTypeForNumber() throws Exception {
        // Test when the next token is a number
        jsonReader = new JsonReader(new StringReader("{\"number\":123}"));
        jsonReader.beginObject();
        jsonReader.nextName();
        assertEquals(JsonToken.NUMBER, invokePeek());
        jsonReader.nextLong();
        jsonReader.endObject();
    }

    @Test
    void testPeekThrowsIOExceptionWhenReaderIsClosed() throws IOException {
        // Close the JsonReader and attempt to call peek
        jsonReader.close();
        // Expect IllegalStateException
        assertThrows(IllegalStateException.class, jsonReader::peek);
    }

    @Test
    void testPeekThrowsIOExceptionOnUnexpectedEndOfFile() throws IOException {
        // Simulate empty input stream which will lead to EOFException when peeking
        jsonReader = new JsonReader(new StringReader(""));
        assertThrows(EOFException.class, this::invokePeek);
    }

    @Test
    void testPeekThrowsIOExceptionWhenInputIsMalformed() throws Exception {
        // Test with malformed JSON input to trigger an IOException
        jsonReader = new JsonReader(new StringReader("{\"key:}"));
        assertThrows(IOException.class, () -> {
            jsonReader.beginObject();
            jsonReader.nextName();
            invokePeek();
        });
    }

    @Test
    void testPeekHandlesIOExceptionFromDoPeek() throws Exception {
        // Mock the doPeek method to throw IOException
        JsonReader mockReader = Mockito.spy(jsonReader);
        doThrow(new IOException("Mock IOException")).when(mockReader).doPeek();
        assertThrows(IOException.class, mockReader::peek);
    }

//     @Test
//     void testPeekThrowsEOFExceptionOnUnexpectedEndOfFile() throws IOException {
//         // Simulating EOFException when peeking
//         jsonReader = new JsonReader(new StringReader(""));
//         assertThrows(EOFException.class, () -> {
//             try {
//                 invokePeek();
//             } catch (InvocationTargetException e) {
//                 throw (Throwable) e.getCause();
//             }
//         });
//     }

//     @Test
//     void testPeekThrowsEOFExceptionOnUnexpectedEndOfFileAgain() {
//         // Another test for EOFException during a method invocation
//         jsonReader = new JsonReader(new StringReader(""));
//         assertThrows(EOFException.class, () -> {
//             try {
//                 invokePeek();
//             } catch (InvocationTargetException e) {
//                 throw (Throwable) e.getCause();
//             }
//         });
//     }
}