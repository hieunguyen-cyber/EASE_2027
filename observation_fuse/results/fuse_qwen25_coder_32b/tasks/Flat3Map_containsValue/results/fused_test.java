package org.apache.commons.collections4.map;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class Flat3Map_containsValue_Test {

    private Flat3Map<Integer, String> flat3Map;

    @BeforeEach
    void setupBeforeEach() {
        flat3Map = new Flat3Map<>();
        flat3Map.put(1, "One");
        flat3Map.put(2, "Two");
        flat3Map.put(3, "Three");
    }

    @AfterEach
    void teardownAfterEach() {
        flat3Map = null;
    }

    // Used reflection for testing private method 'containsValue'
    boolean invokeContainsValue(Flat3Map<Integer, String> map, Object value) {
        try {
            Method method = Flat3Map.class.getDeclaredMethod("containsValue", Object.class);
            method.setAccessible(true);
            return (boolean) method.invoke(map, value);
        } catch (Exception e) {
            fail("Reflection invocation failed: " + e.getMessage());
            // This will never be reached because of the fail above
            return false;
        }
    }

    @Test
    void testContainsValue_ExistingValue() {
        String existingValue = "One";
        boolean result = invokeContainsValue(flat3Map, existingValue);
        assertTrue(result, "Map should contain the value 'One'");
    }

    @Test
    void testContainsValue_NonExistingValue() {
        String nonExistingValue = "Four";
        boolean result = invokeContainsValue(flat3Map, nonExistingValue);
        assertFalse(result, "Map should not contain the value 'Four'");
    }

    @Test
    void testContainsValue_NullValue() {
        flat3Map.put(4, null);
        boolean result = invokeContainsValue(flat3Map, null);
        assertTrue(result, "Map should contain null as a value");
    }

    @Test
    void testContainsValue_NullWithNoValues() {
        flat3Map = new Flat3Map<>();
        boolean result = invokeContainsValue(flat3Map, null);
        assertFalse(result, "Map should not contain any values, hence should return false for null");
    }

    @Test
    void testContainsValue_ExactMatchingNullValue() {
        flat3Map = new Flat3Map<>();
        flat3Map.put(1, null);
        flat3Map.put(2, "Two");
        flat3Map.put(3, "Three");
        boolean result = invokeContainsValue(flat3Map, null);
        assertTrue(result, "Map should contain a null value among its entries");
    }

    @Test
    void testContainsValue_ValueMatchesSize3Value() {
        String matchingValue = "Three";
        boolean result = invokeContainsValue(flat3Map, matchingValue);
        assertTrue(result, "Map should contain the value 'Three'");
    }

    @Test
    void testContainsValue_ValueMatchesSize2Value() {
        flat3Map.put(4, "Four");
        flat3Map.remove(1);
        boolean result = invokeContainsValue(flat3Map, "Two");
        assertTrue(result, "Map should contain the value 'Two'");
    }

    @Test
    void testContainsValue_ValueMatchesSize1Value() {
        flat3Map.put(4, "Four");
        flat3Map.remove(2);
        boolean result = invokeContainsValue(flat3Map, "One");
        assertTrue(result, "Map should contain the value 'One'");
    }

    @Test
    void testContainsValue_AfterClear() {
        flat3Map.clear();
        boolean result = invokeContainsValue(flat3Map, "One");
        assertFalse(result, "After clear, the map should not contain any values");
    }

    @Test
    void testContainsValue_EmptyMap() {
        flat3Map = new Flat3Map<>();
        boolean result = invokeContainsValue(flat3Map, "AnyValue");
        assertFalse(result, "Empty map should not contain any values");
    }

    @Test
    void testContainsValue_ValueAtBoundarySize3() {
        flat3Map.put(4, "Four");
        flat3Map.remove(3);
        boolean result = invokeContainsValue(flat3Map, "One");
        assertTrue(result, "Map should contain the value 'One' after removing another entry");
    }

    @Test
    void testContainsValue_MultipleNullVsAlternativeValues() {
        flat3Map = new Flat3Map<>();
        flat3Map.put(1, null);
        flat3Map.put(2, null);
        flat3Map.put(3, "Three");
        boolean resultNull = invokeContainsValue(flat3Map, null);
        boolean resultThree = invokeContainsValue(flat3Map, "Three");
        assertTrue(resultNull, "Map should contain null among its entries");
        assertTrue(resultThree, "Map should contain the value 'Three'");
    }
@Test
void test_nullValueWithNullValue3() {
    flat3Map = new Flat3Map<>();
    flat3Map.put(1, "One");
    flat3Map.put(2, "Two");
    flat3Map.put(3, null);
    boolean result = invokeContainsValue(flat3Map, null);
    assertTrue(result, "Map should contain null as value3");
}
@Test
void test_nullValueWithNullValue2() {
    flat3Map = new Flat3Map<>();
    flat3Map.put(1, "One");
    flat3Map.put(2, null);
    boolean result = invokeContainsValue(flat3Map, null);
    assertTrue(result, "Map should contain null as value2");
}
@Test
void test_nonNullValueMatchesValue2() {
    flat3Map = new Flat3Map<>();
    flat3Map.put(1, "One");
    flat3Map.put(2, "Two");
    boolean result = invokeContainsValue(flat3Map, "Two");
    assertTrue(result, "Map should contain 'Two' as value2");
}
@Test
void test_delegateMapContainsValueTrue() {
    flat3Map = new Flat3Map<>();
    flat3Map.put(1, "One");
    flat3Map.put(2, "Two");
    flat3Map.put(3, "Three");
    flat3Map.put(4, "Four");
    boolean result = invokeContainsValue(flat3Map, "Four");
    assertTrue(result, "Delegate map should contain 'Four'");
}
}