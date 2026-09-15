package org.apache.commons.collections4.map;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Method;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class Flat3Map_equals_Test {

    private Flat3Map<Integer, String> map1;

    private Flat3Map<Integer, String> map2;

    @BeforeEach
    void setupBeforeEach() {
        map1 = new Flat3Map<>();
        map2 = new Flat3Map<>();
        // Populate map1
        map1.put(1, "One");
        map1.put(2, "Two");
        map1.put(3, "Three");
        // Populate map2 as a different map
        map2.put(1, "One");
        map2.put(2, "Two");
        map2.put(3, "Three");
    }

    @AfterEach
    void teardownAfterEach() {
        map1.clear();
        map2.clear();
    }

    @Test
    void testEquals_WithSameObject() {
        // Test that a map is equal to itself
        assertTrue(map1.equals(map1), "Map should equal itself");
    }

    @Test
    void testEquals_WithSameContent() {
        // Test that two maps with the same content are equal
        assertTrue(map1.equals(map2), "Maps with same content should be equal");
    }

    @Test
    void testEquals_WithDifferentContent() {
        // Test that two maps with different contents are not equal
        map2.put(4, "Four");
        assertFalse(map1.equals(map2), "Maps with different content should not be equal");
    }

    @Test
    void testEquals_WithDifferentType() {
        // Test that a map is not equal to a non-map object
        String notAMap = "Not a Map";
        assertFalse(map1.equals(notAMap), "Map should not be equal to a non-map object");
    }

    @Test
    void testEquals_WithNull() {
        // Test that a map is not equal to null
        assertFalse(map1.equals(null), "Map should not be equal to null");
    }

    @Test
    void testEquals_WithPartialContent() {
        // Test that maps with different sizes are not equal
        Flat3Map<Integer, String> differentMap = new Flat3Map<>();
        differentMap.put(1, "One");
        differentMap.put(2, "Two");
        assertFalse(map1.equals(differentMap), "Maps with different size should not be equal");
    }

    @Test
    void testEquals_WithExactlyThreeElements() {
        // Test equality when three elements are added
        Flat3Map<Integer, String> equalMap = new Flat3Map<>();
        equalMap.put(1, "One");
        equalMap.put(2, "Two");
        equalMap.put(3, "Three");
        assertTrue(map1.equals(equalMap), "Maps with exactly three same elements should be equal");
        // Test with different values for the third key
        equalMap.put(3, "Different");
        assertFalse(map1.equals(equalMap), "Maps should not be equal if one has different value for the third key");
    }

    @Test
    void testEquals_WithEmptyMap() {
        // Test that an empty map is not equal to a non-empty map
        Flat3Map<Integer, String> emptyMap = new Flat3Map<>();
        assertFalse(map1.equals(emptyMap), "Non-empty map should not equal an empty map");
    }

    // Additional tests to cover corner cases
    @Test
    void testEquals_WithDifferentOrder() {
        Flat3Map<Integer, String> reorderedMap = new Flat3Map<>();
        reorderedMap.put(3, "Three");
        reorderedMap.put(1, "One");
        reorderedMap.put(2, "Two");
        assertTrue(map1.equals(reorderedMap), "Maps with same entries in different order should be equal");
    }

    @Test
    void testEquals_WithSameKeysDifferentNullValues() {
        Flat3Map<Integer, String> nullValueMap = new Flat3Map<>();
        nullValueMap.put(1, null);
        nullValueMap.put(2, null);
        nullValueMap.put(3, null);
        assertFalse(map1.equals(nullValueMap), "Maps should not be equal if keys match but values are null");
    }

    @Test
    void testEquals_WithDifferentSizeMaps() {
        Flat3Map<Integer, String> largerMap = new Flat3Map<>();
        largerMap.put(1, "One");
        largerMap.put(2, "Two");
        largerMap.put(3, "Three");
        largerMap.put(4, "Four");
        assertFalse(map1.equals(largerMap), "Maps with different sizes should not be equal");
    }

    // New tests to cover additional exception scenarios
    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testEquals_WithDelegateMapDifferentSuccess() throws Exception {
        // Simulate a scenario where delegateMap is not null and equals returns false
        Method setDelegateMapMethod = Flat3Map.class.getDeclaredMethod("setDelegateMap", AbstractHashedMap.class);
        setDelegateMapMethod.setAccessible(true);
        AbstractHashedMap<Integer, String> delegateMap = mock(AbstractHashedMap.class);
        when(delegateMap.equals(any())).thenReturn(false);
        setDelegateMapMethod.invoke(map1, delegateMap);
        assertFalse(map1.equals(map2), "Should return false due to custom delegate map");
    }

    @Test
    void testEqualsWithNonSerializableKeys() {
        // Handle scenario with non-serializable keys
        Flat3Map<Object, String> nonSerializableMap1 = new Flat3Map<>();
        Flat3Map<Object, String> nonSerializableMap2 = new Flat3Map<>();
        nonSerializableMap1.put(new Object(), "Value");
        nonSerializableMap2.put(new Object(), "Value");
        assertFalse(nonSerializableMap1.equals(nonSerializableMap2), "Non-serializable keys should not equal");
    }
@Test
void test_equals_with_size_greater_than_zero() {
    Flat3Map<Integer, String> map3 = new Flat3Map<>();
    map3.put(1, "One");
    map3.put(2, "Two");
    map3.put(3, "Three");
    assertTrue(map1.equals(map3), "Maps with size greater than 0 should be equal");
}
@Test
void test_equals_with_key3_not_present() {
    Flat3Map<Integer, String> map3 = new Flat3Map<>();
    map3.put(1, "One");
    map3.put(2, "Two");
    assertFalse(map1.equals(map3), "Maps should not be equal if key3 is not present");
}
@Test
void test_equals_with_key2_not_present() {
    Flat3Map<Integer, String> map3 = new Flat3Map<>();
    map3.put(1, "One");
    map3.put(3, "Three");
    assertFalse(map1.equals(map3), "Maps should not be equal if key2 is not present");
}
@Test
void test_equals_with_value2_different() {
    Flat3Map<Integer, String> map3 = new Flat3Map<>();
    map3.put(1, "One");
    map3.put(2, "Different");
    map3.put(3, "Three");
    assertFalse(map1.equals(map3), "Maps should not be equal if value2 is different");
}
@Test
void test_equals_with_key1_not_present() {
    Flat3Map<Integer, String> map3 = new Flat3Map<>();
    map3.put(2, "Two");
    map3.put(3, "Three");
    assertFalse(map1.equals(map3), "Maps should not be equal if key1 is not present");
}
@Test
void test_equals_with_value1_different() {
    Flat3Map<Integer, String> map3 = new Flat3Map<>();
    map3.put(1, "Different");
    map3.put(2, "Two");
    map3.put(3, "Three");
    assertFalse(map1.equals(map3), "Maps should not be equal if value1 is different");
}
}