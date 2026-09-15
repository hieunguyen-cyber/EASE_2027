package org.apache.commons.collections4.map;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class Flat3Map_containsKey_Test {

    private Flat3Map<Integer, String> flat3Map;

    @BeforeEach
    void setupBeforeEach() {
        flat3Map = new Flat3Map<>();
        flat3Map.put(1, "One");
        flat3Map.put(2, "Two");
        flat3Map.put(3, "Three");
    }

//     private void setDelegateMap(Flat3Map<Integer, String> flat3Map, Flat3Map<Integer, String> delegateMap) throws Exception {
//         // Ensure that the method name "setDelegateMap" matches the actual method in Flat3Map
//         Method method = Flat3Map.class.getDeclaredMethod("setDelegateMap", Flat3Map.class);
//         method.setAccessible(true);
//         method.invoke(flat3Map, delegateMap);
//     }

    @Test
    void testContainsKey_withExistingKey() {
        assertTrue(flat3Map.containsKey(1), "Expected to find key 1 in the map.");
        assertTrue(flat3Map.containsKey(2), "Expected to find key 2 in the map.");
        assertTrue(flat3Map.containsKey(3), "Expected to find key 3 in the map.");
    }

    @Test
    void testContainsKey_withNonExistingKey() {
        assertFalse(flat3Map.containsKey(4), "Did not expect to find key 4 in the map.");
    }

    @Test
    void testContainsKey_withNullKey() {
        flat3Map.put(null, "Null");
        assertTrue(flat3Map.containsKey(null), "Expected to find null key in the map.");
    }

    @Test
    void testContainsKey_withNullValue() {
        flat3Map.put(4, null);
        assertFalse(flat3Map.containsKey(5), "Did not expect to find non-existing key 5 in the map.");
    }

    @Test
    void testContainsKey_withNullKey_whenKeysAreNull() {
        flat3Map.put(null, "Null");
        assertTrue(flat3Map.containsKey(null), "Expected to find null key in the map.");
    }

    @Test
    void testContainsKey_withNullKey_whenKeysAreNotNull() {
        flat3Map.clear();
        assertFalse(flat3Map.containsKey(null), "Did not expect to find null key in an empty map.");
    }

    @Test
    void testContainsKey_withNullKey_withMixedKeys() {
        flat3Map.put(4, null);
        assertFalse(flat3Map.containsKey(5), "Did not expect to find non-existing key 5 in the map.");
    }

    @Test
    void testContainsKey_withNull_whenSizeIsOne() {
        flat3Map.clear();
        flat3Map.put(null, "OnlyNull");
        assertTrue(flat3Map.containsKey(null), "Expected to find null key in the map with one null key.");
    }

    @Test
    void testContainsKey_withNull_whenSizeIsTwo() {
        flat3Map.clear();
        flat3Map.put(null, "NullValue");
        flat3Map.put(1, "One");
        assertTrue(flat3Map.containsKey(null), "Expected to find null key in the map with null and one key.");
    }

    @Test
    void testContainsKey_withNull_whenSizeIsThree() {
        flat3Map.clear();
        flat3Map.put(1, "One");
        flat3Map.put(2, "Two");
        flat3Map.put(null, "Three");
        assertTrue(flat3Map.containsKey(null), "Expected to find null key in the map with three keys containing null.");
    }

//     @Test
//     void testContainsKey_whenDelegateMapIsNonNull() throws Exception {
//         Flat3Map<Integer, String> delegateMap = mock(Flat3Map.class);
//         flat3Map.put(1, "One");
//         flat3Map.put(2, "Two");
//         flat3Map.put(3, "Three");
//         // Simulating delegation behavior rather than using setDelegateMap
//         when(delegateMap.containsKey(1)).thenReturn(true);
//         flat3Map.put(4, delegateMap);
//         assertTrue(flat3Map.containsKey(1), "Expected to find key 1 via delegateMap.");
//         verify(delegateMap).containsKey(1);
//     }

//     @Test
//     void testContainsKey_withExistingKey_withDelegation() throws Exception {
//         Flat3Map<Integer, String> delegateMap = mock(Flat3Map.class);
//         when(delegateMap.containsKey(2)).thenReturn(true);
//         flat3Map.put(4, delegateMap);
//         assertTrue(flat3Map.containsKey(2), "Expected to find key 2 via delegateMap.");
//         verify(delegateMap).containsKey(2);
//     }

//     @Test
//     void testContainsKey_whenDelegateMapThrowsException() throws Exception {
//         // Adjusting to not use setDelegateMap and directly mock behavior
//         Flat3Map<Integer, String> delegateMap = mock(Flat3Map.class);
//         when(delegateMap.containsKey(any())).thenThrow(new RuntimeException("Mocked Exception"));
//         RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
//             flat3Map.put(4, delegateMap);
//             flat3Map.containsKey(1);
//         });
//         assertEquals("Mocked Exception", thrown.getMessage(), "Expected RuntimeException to be thrown.");
//     }

//     @Test
//     void testContainsKey_withNonSerializableDelegateMap() throws Exception {
//         Flat3Map<Integer, String> nonSerializableMap = mock(Flat3Map.class);
//         when(nonSerializableMap.containsKey(any())).thenThrow(new IOException("Not Serializable"));
//         IOException thrown = assertThrows(IOException.class, () -> {
//             flat3Map.put(4, nonSerializableMap);
//             flat3Map.containsKey(1);
//         });
//         assertEquals("Not Serializable", thrown.getMessage(), "Expected IOException when using non-serializable delegateMap.");
//     }
@Test
void testContainsKey_withSizeGreaterThanZeroAndKey2IsNull() {
    flat3Map.clear();
    flat3Map.put(1, "One");
    flat3Map.put(null, "Two");
    assertTrue(flat3Map.containsKey(null), "Expected to find null key in the map with size greater than 0 and key2 is null.");
}
@Test
void testContainsKey_withSizeGreaterThanZeroAndKey2IsNotNull() {
    flat3Map.clear();
    flat3Map.put(1, "One");
    flat3Map.put(2, "Two");
    assertFalse(flat3Map.containsKey(null), "Did not expect to find null key in the map with size greater than 0 and key2 is not null.");
}
@Test
void testContainsKey_withSizeGreaterThanZeroAndKey1IsNull() {
    flat3Map.clear();
    flat3Map.put(null, "One");
    assertTrue(flat3Map.containsKey(null), "Expected to find null key in the map with size greater than 0 and key1 is null.");
}
@Test
void testContainsKey_withSizeGreaterThanZeroAndKey1IsNotNull() {
    flat3Map.clear();
    flat3Map.put(1, "One");
    assertFalse(flat3Map.containsKey(null), "Did not expect to find null key in the map with size greater than 0 and key1 is not null.");
}
@Test
void testContainsKey_withSizeGreaterThanZeroAndKey3IsNull() {
    flat3Map.clear();
    flat3Map.put(1, "One");
    flat3Map.put(2, "Two");
    flat3Map.put(null, "Three");
    assertTrue(flat3Map.containsKey(null), "Expected to find null key in the map with size greater than 0 and key3 is null.");
}
@Test
void testContainsKey_withSizeGreaterThanZeroAndKey3IsNotNull() {
    flat3Map.clear();
    flat3Map.put(1, "One");
    flat3Map.put(2, "Two");
    flat3Map.put(3, "Three");
    assertFalse(flat3Map.containsKey(null), "Did not expect to find null key in the map with size greater than 0 and key3 is not null.");
}
@Test
void testContainsKey_withSizeGreaterThanZeroAndKey2EqualsGivenKey() {
    flat3Map.clear();
    flat3Map.put(1, "One");
    flat3Map.put(2, "Two");
    assertTrue(flat3Map.containsKey(2), "Expected to find key 2 in the map with size greater than 0 and key2 equals the given key.");
}
@Test
void testContainsKey_withSizeGreaterThanZeroAndKey2DoesNotEqualGivenKey() {
    flat3Map.clear();
    flat3Map.put(1, "One");
    flat3Map.put(2, "Two");
    assertFalse(flat3Map.containsKey(3), "Did not expect to find key 3 in the map with size greater than 0 and key2 does not equal the given key.");
}
@Test
void testContainsKey_withSizeGreaterThanZeroAndKey1EqualsGivenKey() {
    flat3Map.clear();
    flat3Map.put(1, "One");
    assertTrue(flat3Map.containsKey(1), "Expected to find key 1 in the map with size greater than 0 and key1 equals the given key.");
}
@Test
void testContainsKey_withSizeGreaterThanZeroAndKey1DoesNotEqualGivenKey() {
    flat3Map.clear();
    flat3Map.put(1, "One");
    assertFalse(flat3Map.containsKey(2), "Did not expect to find key 2 in the map with size greater than 0 and key1 does not equal the given key.");
}
@Test
void testContainsKey_withSizeGreaterThanZeroAndKey3EqualsGivenKey() {
    flat3Map.clear();
    flat3Map.put(1, "One");
    flat3Map.put(2, "Two");
    flat3Map.put(3, "Three");
    assertTrue(flat3Map.containsKey(3), "Expected to find key 3 in the map with size greater than 0 and key3 equals the given key.");
}
@Test
void testContainsKey_withSizeGreaterThanZeroAndKey3DoesNotEqualGivenKey() {
    flat3Map.clear();
    flat3Map.put(1, "One");
    flat3Map.put(2, "Two");
    flat3Map.put(3, "Three");
    assertFalse(flat3Map.containsKey(4), "Did not expect to find key 4 in the map with size greater than 0 and key3 does not equal the given key.");
}
}