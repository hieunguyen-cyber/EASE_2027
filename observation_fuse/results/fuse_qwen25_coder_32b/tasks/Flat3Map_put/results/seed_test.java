package org.apache.commons.collections4.map;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Method;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.lang.reflect.Field;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class Flat3Map_put_Test {

    private Flat3Map<Integer, String> flat3Map;

    @BeforeEach
    void setupBeforeEach() {
        flat3Map = new Flat3Map<>();
    }

    @AfterEach
    void teardownAfterEach() {
        flat3Map.clear();
    }

    private static Object[][] keyValuePairs() {
        return new Object[][] { { 1, "One", null, 1 }, { 2, "Two", null, 2 }, { 1, "One", "One", 2 }, { 2, null, "Two", 2 } };
    }

    @Test
    void testPut_NewEntry() {
        assertNull(flat3Map.put(1, "One"));
        assertEquals(1, flat3Map.size());
        assertEquals("One", flat3Map.get(1));
    }

    @Test
    void testPut_ExistingEntry() {
        flat3Map.put(1, "One");
        assertEquals("One", flat3Map.put(1, "Updated One"));
        assertEquals(1, flat3Map.size());
        assertEquals("Updated One", flat3Map.get(1));
    }

    @Test
    void testPut_NullKey() {
        flat3Map.put(1, "One");
        assertNull(flat3Map.put(null, "Updated Null"));
        assertEquals("Updated Null", flat3Map.get(null));
        assertEquals(2, flat3Map.size());
    }

    @Test
    void testPut_ExceedingSize() {
        flat3Map.put(1, "One");
        flat3Map.put(2, "Two");
        flat3Map.put(3, "Three");
        flat3Map.put(4, "Four");
        assertEquals(4, flat3Map.size());
        assertEquals("Four", flat3Map.get(4));
    }

    @Test
    void testPut_SpecialCases() {
        flat3Map.put(1, "One");
        flat3Map.put(2, "Two");
        // Returns the old value
        assertEquals("One", flat3Map.put(1, null));
        // New entry with null value
        assertNull(flat3Map.put(3, null));
        assertEquals(3, flat3Map.size());
        // Value for key 1 should be null
        assertNull(flat3Map.get(1));
    }

    @Test
    void testPut_WithFullMapThenClear() {
        flat3Map.put(1, "One");
        flat3Map.put(2, "Two");
        flat3Map.put(3, "Three");
        flat3Map.clear();
        assertEquals(0, flat3Map.size());
        assertNull(flat3Map.put(4, "Four"));
        assertEquals(1, flat3Map.size());
        assertEquals("Four", flat3Map.get(4));
    }

    @Test
    void testPut_UpdatingWithDifferentKeys() {
        flat3Map.put(1, "One");
        flat3Map.put(2, "Two");
        // Returns old value
        assertEquals("One", flat3Map.put(1, "New One"));
        // Returns old value
        assertEquals("Two", flat3Map.put(2, "New Two"));
        assertEquals(2, flat3Map.size());
        assertEquals("New One", flat3Map.get(1));
        assertEquals("New Two", flat3Map.get(2));
    }

    @ParameterizedTest
    @MethodSource("keyValuePairs")
    void testPut_Parameterized(Integer key, String value, String expectedOldValue, int expectedSize) {
        String oldValue = flat3Map.put(key, value);
        if (expectedOldValue != null) {
            // Check old value
            assertEquals(expectedOldValue, oldValue);
        } else {
            // Check if old value was null
            assertNull(oldValue);
        }
        // Check size
        assertEquals(expectedSize, flat3Map.size());
    }

    @Test
    void testPut_WhenDelegateMapIsNonNull() throws Exception {
        Field delegateMapField = Flat3Map.class.getDeclaredField("delegateMap");
        delegateMapField.setAccessible(true);
        // Set up mock for delegateMap
        AbstractHashedMap<Integer, String> mockDelegateMap = mock(AbstractHashedMap.class);
        delegateMapField.set(flat3Map, mockDelegateMap);
        when(mockDelegateMap.put(any(), any())).thenReturn("Previous Value");
        String previousValue = flat3Map.put(1, "One");
        verify(mockDelegateMap).put(1, "One");
        assertEquals("Previous Value", previousValue);
    }

    @Test
    void testPut_DelegateMapThrowsException() throws Exception {
        Field delegateMapField = Flat3Map.class.getDeclaredField("delegateMap");
        delegateMapField.setAccessible(true);
        AbstractHashedMap<Integer, String> mockDelegateMap = mock(AbstractHashedMap.class);
        delegateMapField.set(flat3Map, mockDelegateMap);
        when(mockDelegateMap.put(any(), any())).thenThrow(new RuntimeException("Delegate map error"));
        Exception exception = assertThrows(RuntimeException.class, () -> {
            flat3Map.put(1, "One");
        });
        assertEquals("Delegate map error", exception.getMessage());
    }
}
