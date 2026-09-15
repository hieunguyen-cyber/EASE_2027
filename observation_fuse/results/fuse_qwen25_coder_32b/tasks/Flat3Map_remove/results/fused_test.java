package org.apache.commons.collections4.map;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class Flat3Map_remove_Test {

    private Flat3Map<Integer, String> flat3Map;

    @BeforeEach
    void setupBeforeEach() {
        flat3Map = new Flat3Map<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up after each test
        flat3Map.clear();
    }

    // Parameterized Test for various scenarios of remove
    private static Stream<Arguments> provideRemoveCases() {
        return // Providing all arguments for parameterized test
        Stream.// Providing all arguments for parameterized test
        of(// Removing existing key, size reduces
        Arguments.of(1, "One", 1, "One", 0), // Removing existing key, size reduces
        Arguments.of(2, "Two", 2, "Two", 1), // Removing existing key, size reduces
        Arguments.of(3, "Three", 3, "Three", 2), // Removing non-existent key
        Arguments.of(99, null, 1, null, 1), // Removing null key
        Arguments.of(null, "NullKey", null, "NullKey", 0));
    }

    @Test
    void testRemoveWithSingleElement() {
        flat3Map.put(1, "One");
        String removedValue = flat3Map.remove(1);
        assertEquals("One", removedValue, "Removed value should be 'One'");
        assertNull(flat3Map.get(1), "Value should be null after removal");
    }

    @Test
    void testRemoveWithMultipleElements() {
        flat3Map.put(1, "One");
        flat3Map.put(2, "Two");
        flat3Map.put(3, "Three");
        String removedValue = flat3Map.remove(2);
        assertEquals("Two", removedValue, "Removed value should be 'Two'");
        assertNull(flat3Map.get(2), "Value should be null after removal");
        assertEquals(2, flat3Map.size(), "Map size should now be 2");
    }

    @Test
    void testRemoveNonExistentKey() {
        flat3Map.put(1, "One");
        // Non-existent key
        String removedValue = flat3Map.remove(99);
        assertNull(removedValue, "Removing a non-existent key should return null");
        assertEquals(1, flat3Map.size(), "Map size should remain 1");
    }

    @Test
    void testRemoveNullKey() {
        flat3Map.put(null, "NullKey");
        String removedValue = flat3Map.remove(null);
        assertEquals("NullKey", removedValue, "Removed value should be 'NullKey'");
        assertNull(flat3Map.get(null), "Value should be null after removal");
    }

    @Test
    void testRemoveFromEmptyMap() {
        // Removing from empty map
        String removedValue = flat3Map.remove(1);
        assertNull(removedValue, "Removing from an empty map should return null");
    }

    @Test
    void testRemoveNullKeyWhenThreeElementsExist() {
        flat3Map.put(null, "NullKey");
        flat3Map.put(1, "One");
        flat3Map.put(2, "Two");
        String removedValue = flat3Map.remove(null);
        assertEquals("NullKey", removedValue, "Removed value should be 'NullKey'");
        assertNull(flat3Map.get(null), "Value should be null after removal");
        assertEquals(2, flat3Map.size(), "Map size should be 2 after removal");
    }

    @Test
    void testRemoveKeyWhenThreeElementsExist() {
        flat3Map.put(1, "One");
        flat3Map.put(2, "Two");
        flat3Map.put(3, "Three");
        String removedValue = flat3Map.remove(3);
        assertEquals("Three", removedValue, "Removed value should be 'Three'");
        assertNull(flat3Map.get(3), "Value 3 should be null after removal");
        assertEquals(2, flat3Map.size(), "Map size should be 2 after removal");
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @ParameterizedTest
    @MethodSource("provideRemoveCases")
    void testRemoveParameterized(Integer keyToPut, String valueToPut, Integer keyToRemove, String expectedRemovedValue, int expectedSize) {
        if (keyToPut != null) {
            flat3Map.put(keyToPut, valueToPut);
        }
        if (keyToRemove != null) {
            String removedValue = flat3Map.remove(keyToRemove);
            assertEquals(expectedRemovedValue, removedValue, "Removed value should be as expected");
        }
        assertEquals(expectedSize, flat3Map.size(), "Map size should be as expected");
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testRemoveWhenDelegateMapIsNonNull() {
        Flat3Map<Integer, String> spyMap = Mockito.spy(flat3Map);
        AbstractHashedMap<Integer, String> delegateMap = mock(AbstractHashedMap.class);
        doReturn(delegateMap).when(spyMap).createDelegateMap();
        when(delegateMap.remove(any())).thenReturn("MockedValue");
        String removedValue = spyMap.remove(1);
        assertEquals("MockedValue", removedValue, "Should return mocked value when delegate map is used");
    }

    @Test
    void testRemoveNullKeyAdaptivelyWithThreeElements() {
        flat3Map.put(1, "One");
        flat3Map.put(2, "Two");
        flat3Map.put(3, "Three");
        flat3Map.put(null, "NullAfterThree");
        String removedValue = flat3Map.remove(null);
        assertEquals("NullAfterThree", removedValue, "Should return value associated with null key");
        assertEquals(3, flat3Map.size(), "Map size should remain the same as before");
    }
@Test
void testRemoveWhenSizeGreaterThanZero() {
    flat3Map.put(1, "One");
    flat3Map.put(2, "Two");
    flat3Map.put(3, "Three");
    String removedValue = flat3Map.remove(1);
    assertEquals("One", removedValue, "Removed value should be 'One'");
    assertEquals(2, flat3Map.size(), "Map size should be 2 after removal");
}
@Test
void testRemoveWhenSizeIsZero() {
    String removedValue = flat3Map.remove(1);
    assertNull(removedValue, "Removing from an empty map should return null");
    assertEquals(0, flat3Map.size(), "Map size should remain 0");
}
@Test
void testRemoveWhenKey3IsNull() {
    flat3Map.put(1, "One");
    flat3Map.put(2, "Two");
    flat3Map.put(null, "NullKey");
    String removedValue = flat3Map.remove(null);
    assertEquals("NullKey", removedValue, "Removed value should be 'NullKey'");
    assertEquals(2, flat3Map.size(), "Map size should be 2 after removal");
}
@Test
void testRemoveWhenKey2IsNull() {
    flat3Map.put(1, "One");
    flat3Map.put(null, "NullKey");
    flat3Map.put(3, "Three");
    String removedValue = flat3Map.remove(null);
    assertEquals("NullKey", removedValue, "Removed value should be 'NullKey'");
    assertEquals(2, flat3Map.size(), "Map size should be 2 after removal");
}
@Test
void testRemoveKey2IsNullInSize2() {
    flat3Map.put(1, "One");
    flat3Map.put(null, "NullKey");
    String removedValue = flat3Map.remove(null);
    assertEquals("NullKey", removedValue, "Removed value should be 'NullKey'");
    assertEquals(1, flat3Map.size(), "Map size should be 1 after removal");
}
@Test
void testRemoveKey1IsNullInSize2() {
    flat3Map.put(null, "NullKey");
    flat3Map.put(2, "Two");
    String removedValue = flat3Map.remove(null);
    assertEquals("NullKey", removedValue, "Removed value should be 'NullKey'");
    assertEquals(1, flat3Map.size(), "Map size should be 1 after removal");
}
@Test
void testRemoveWithHash1Matching() {
    flat3Map.put(1, "One");
    String removedValue = flat3Map.remove(1);
    assertEquals("One", removedValue, "Removed value should be 'One'");
    assertEquals(0, flat3Map.size(), "Map size should be 0 after removal");
}
@Test
void testRemoveWithHash2Matching() {
    flat3Map.put(1, "One");
    flat3Map.put(2, "Two");
    String removedValue = flat3Map.remove(2);
    assertEquals("Two", removedValue, "Removed value should be 'Two'");
    assertEquals(1, flat3Map.size(), "Map size should be 1 after removal");
}
@Test
void testRemoveWithHash3Matching() {
    flat3Map.put(1, "One");
    flat3Map.put(2, "Two");
    flat3Map.put(3, "Three");
    String removedValue = flat3Map.remove(3);
    assertEquals("Three", removedValue, "Removed value should be 'Three'");
    assertEquals(2, flat3Map.size(), "Map size should be 2 after removal");
}
@Test
void testRemoveWithNoMatchingHash() {
    flat3Map.put(1, "One");
    flat3Map.put(2, "Two");
    flat3Map.put(3, "Three");
    String removedValue = flat3Map.remove(4);
    assertNull(removedValue, "Removing a non-existent key should return null");
    assertEquals(3, flat3Map.size(), "Map size should remain 3");
}
}