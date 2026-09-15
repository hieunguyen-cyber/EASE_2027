package com.google.gson.internal.bind;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonNull;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.lang.reflect.Method;
import com.google.gson.JsonPrimitive;
import org.mockito.junit.jupiter.MockitoExtension;
import com.google.gson.stream.MalformedJsonException;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class JsonTreeReader_peek_Test {

    private JsonTreeReader reader;

    @BeforeEach
    void setUp() {
        // Reinitialize reader before each test if needed
        reader = null;
    }

    private JsonElement createCustomJsonElement() {
        // Placeholder for creating a custom JsonElement instance which will trigger MalformedJsonException
        return new JsonElement() {

            @Override
            public JsonElement deepCopy() {
                // just return itself for this example
                return this;
            }
        };
    }

    @Test
    void testPeek_emptyJsonObject() throws IOException {
        // Test case for peeking an empty JSON Object
        reader = new JsonTreeReader(new JsonObject());
        // Start reading the object
        reader.beginObject();
        // Now we expect to look at the next token after beginning the object
        assertEquals(JsonToken.END_OBJECT, reader.peek(), "Peek should return END_OBJECT for empty JSON object");
    }

    @Test
    void testPeek_filledJsonObject() throws IOException {
        // Test case for peeking a filled JSON Object
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", "John");
        reader = new JsonTreeReader(jsonObject);
        // Start reading the JSON object
        reader.beginObject();
        assertEquals(JsonToken.NAME, reader.peek(), "Peek should return NAME when there are properties in the JSON object");
        // Move to the next token (the value associated with "name")
        reader.nextName();
        assertEquals(JsonToken.STRING, reader.peek(), "Peek should return STRING after getting the name token");
    }

    @Test
    void testPeek_emptyJsonArray() throws IOException {
        // Test case for peeking an empty JSON Array
        reader = new JsonTreeReader(new JsonArray());
        // Start reading the array
        reader.beginArray();
        // We expect to look at the next token after beginning the array
        assertEquals(JsonToken.END_ARRAY, reader.peek(), "Peek should return END_ARRAY for empty JSON array");
    }

    @Test
    void testPeek_withJsonNull() throws IOException {
        // Test case for peeking a JSON null value
        reader = new JsonTreeReader(JsonNull.INSTANCE);
        assertEquals(JsonToken.NULL, reader.peek(), "Peek should return NULL for JSON null value");
    }

    @Test
    void testPeek_iteratorInJsonObject() throws IOException {
        // Test case to check peek with an iterator in a JSON object
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("key1", new JsonPrimitive("value1"));
        jsonObject.add("key2", new JsonPrimitive(123));
        reader = new JsonTreeReader(jsonObject);
        // Start reading the JSON object
        reader.beginObject();
        // Access the first token (key)
        assertEquals(JsonToken.NAME, reader.peek(), "Peek should return NAME for first key in the object");
    }

    @Test
    void testPeek_iteratorInJsonArray() throws IOException {
        // Test case to check peek with an iterator in a JSON array
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(new JsonPrimitive("value1"));
        jsonArray.add(new JsonPrimitive(123));
        reader = new JsonTreeReader(jsonArray);
        // Start reading the JSON array
        reader.beginArray();
        assertEquals(JsonToken.STRING, reader.peek(), "Peek should return STRING for the first value in the array");
        // Move to the next element
        reader.nextJsonElement();
        assertEquals(JsonToken.NUMBER, reader.peek(), "Peek should return NUMBER for the second value in the array");
    }

    @Test
    void testPeek_customJsonElement() throws Exception {
        // Test for peeking with a custom JsonElement that should throw MalformedJsonException
        Method peekMethod = JsonTreeReader.class.getDeclaredMethod("peek");
        peekMethod.setAccessible(true);
        // Create a custom JsonElement that will lead to a MalformedJsonException
        reader = new JsonTreeReader(createCustomJsonElement());
        assertThrows(MalformedJsonException.class, () -> reader.peek(), "Should throw MalformedJsonException for custom JsonElement class");
    }

    @Test
    void testPeek_afterClose() throws IOException {
        // Test for peeking after the reader has been closed
        reader = new JsonTreeReader(new JsonObject());
        // Close the reader
        reader.close();
        assertThrows(IllegalStateException.class, () -> reader.peek(), "Should throw IllegalStateException when peek is called on a closed JsonReader");
    }
}
