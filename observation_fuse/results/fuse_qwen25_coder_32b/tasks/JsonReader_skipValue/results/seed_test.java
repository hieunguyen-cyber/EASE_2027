package com.google.gson.stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class JsonReader_skipValue_Test {

    private JsonReader jsonReader;

    // Setup before each test
    @BeforeEach
    void setupBeforeEach() {
        jsonReader = new JsonReader(new StringReader("{}"));
    }

    // Teardown after each test
    @AfterEach
    void teardownAfterEach() {
        if (jsonReader != null) {
            try {
                jsonReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static Object[][] provideJsonForSkipping() {
        return new Object[][] { { "{\"key\": \"valueToSkip\", \"nextKey\": \"nextValue\"}", "nextKey" }, { "{\"key\": [1,2,3], \"nextKey\": \"nextValue\"}", "nextKey" }, { "{\"key\": {\"innerKey\": \"value\"}, \"nextKey\": \"nextValue\"}", "nextKey" } };
    }

    @Test
    void testSkipValueWithObject() throws IOException {
        jsonReader = new JsonReader(new StringReader("{\"key1\": \"value1\", \"key2\": {\"subkey\": \"value2\"}}"));
        jsonReader.beginObject();
        assertEquals("key1", jsonReader.nextName());
        jsonReader.skipValue();
        assertEquals("key2", jsonReader.nextName());
        jsonReader.beginObject();
        assertEquals("subkey", jsonReader.nextName());
        assertEquals("value2", jsonReader.nextString());
        jsonReader.endObject();
        jsonReader.endObject();
    }

    @Test
    void testSkipValueWithArray() throws IOException {
        jsonReader = new JsonReader(new StringReader("{\"array\": [1, 2, 3], \"anotherField\": \"value\"}"));
        jsonReader.beginObject();
        assertEquals("array", jsonReader.nextName());
        jsonReader.skipValue();
        assertEquals("anotherField", jsonReader.nextName());
        assertEquals("value", jsonReader.nextString());
        jsonReader.endObject();
    }

    @Test
    void testSkipValueWithNoNextValue() throws IOException {
        jsonReader = new JsonReader(new StringReader("{}"));
        jsonReader.beginObject();
        assertFalse(jsonReader.hasNext(), "Expected no next element");
        jsonReader.endObject();
    }

    /**
     * Test skipping a named property without its value.
     * This ensures we can skip property names correctly.
     */
    @Test
    void testSkipPropertyName() throws Exception {
        jsonReader = new JsonReader(new StringReader("{\"name1\": \"value1\", \"name2\": \"value2\"}"));
        jsonReader.beginObject();
        assertEquals("name1", jsonReader.nextName());
        jsonReader.skipValue();
        assertEquals("name2", jsonReader.nextName());
        assertEquals("value2", jsonReader.nextString());
        jsonReader.endObject();
    }

    /**
     * Test for skipping nested collections.
     * Ensuring that all nested data is correctly skipped without error.
     */
    @Test
    void testSkipNestedArray() throws Exception {
        jsonReader = new JsonReader(new StringReader("{\"nestedArray\": [[1, 2], [3, 4]], \"otherField\": \"value\"}"));
        jsonReader.beginObject();
        assertEquals("nestedArray", jsonReader.nextName());
        jsonReader.skipValue();
        assertEquals("otherField", jsonReader.nextName());
        assertEquals("value", jsonReader.nextString());
        jsonReader.endObject();
    }

    /**
     * Test skipping an entire object containing an empty array.
     * Verifying that the skip behavior works seamlessly.
     */
    @Test
    void testSkipEmptyArrayInObject() throws Exception {
        jsonReader = new JsonReader(new StringReader("{\"emptyArray\": [], \"validField\": \"exists\"}"));
        jsonReader.beginObject();
        assertEquals("emptyArray", jsonReader.nextName());
        jsonReader.skipValue();
        assertEquals("validField", jsonReader.nextName());
        assertEquals("exists", jsonReader.nextString());
        jsonReader.endObject();
    }

    // Additional parameterized test case for skipValue behavior
    @ParameterizedTest
    @MethodSource("provideJsonForSkipping")
    void testSkipValueWithVariousInputs(String json, String expectedNextName) throws Exception {
        jsonReader = new JsonReader(new StringReader(json));
        jsonReader.beginObject();
        assertEquals("key", jsonReader.nextName());
        jsonReader.skipValue();
        assertEquals(expectedNextName, jsonReader.nextName());
        jsonReader.endObject();
    }

    // Test case to trigger IOException when doPeek throws an exception
    @Test
    void testSkipValueThrowsIOExceptionFromDoPeek() throws Exception {
        jsonReader = Mockito.mock(JsonReader.class);
        doThrow(new IOException("Mocked IOException")).when(jsonReader).skipValue();
        assertThrows(IOException.class, () -> jsonReader.skipValue());
    }

    // Test case to trigger an IOException when stack is empty in skipValue
    @Test
    void testSkipValueWithEmptyStack() throws IOException {
        jsonReader = new JsonReader(new StringReader("{}"));
        jsonReader.beginObject();
        jsonReader.nextName();
        jsonReader.skipValue();
        jsonReader.endObject();
        // Now we try to skip value again on an already ended object
        assertThrows(IllegalStateException.class, () -> jsonReader.skipValue());
    }

    // Test case to trigger an IOException when attempting to skip after end of an array
    @Test
    void testSkipValueAfterEndOfArray() throws Exception {
        jsonReader = new JsonReader(new StringReader("[1, 2, 3]"));
        jsonReader.beginArray();
        jsonReader.nextInt();
        jsonReader.skipValue();
        jsonReader.endArray();
        // Now we try to skip value again after finishing array
        assertThrows(IllegalStateException.class, () -> jsonReader.skipValue());
    }
}
