package org.apache.commons.collections4.map;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class Flat3Map_get_Test {

    private Flat3Map<Integer, String> flat3Map;

    @BeforeEach
    void setupBeforeEach() {
        flat3Map = new Flat3Map<>();
    }

    @AfterEach
    void teardownAfterEach() {
        flat3Map.clear();
    }

    @Test
    void testGetWithNullKeyWhenEmpty() {
        String result = flat3Map.get(null);
        assertNull(result);
    }

    @Test
    void testGetWithSingleElement() {
        flat3Map.put(1, "Value1");
        String result = flat3Map.get(1);
        assertEquals("Value1", result);
    }

    @Test
    void testGetWithNullKeyWhenContainsNullValue() {
        flat3Map.put(null, "NullValue");
        String result = flat3Map.get(null);
        assertEquals("NullValue", result);
    }

    @Test
    void testGetWithNonExistentKey() {
        flat3Map.put(1, "Value1");
        flat3Map.put(2, "Value2");
        String result = flat3Map.get(3);
        assertNull(result);
    }

    @Test
    void testGetWithMultipleElements() {
        flat3Map.put(1, "Value1");
        flat3Map.put(2, "Value2");
        flat3Map.put(3, "Value3");
        String result1 = flat3Map.get(1);
        String result2 = flat3Map.get(2);
        String result3 = flat3Map.get(3);
        assertEquals("Value1", result1);
        assertEquals("Value2", result2);
        assertEquals("Value3", result3);
    }

    @Test
    void testGetWithNullKeyWhenMultipleElements() {
        flat3Map.put(1, "Value1");
        flat3Map.put(2, "Value2");
        flat3Map.put(3, "Value3");
        flat3Map.put(null, "NullValue");
        String result = flat3Map.get(null);
        assertEquals("NullValue", result);
    }

    @Test
    void testGetWithNullKeyWhenSizeThree() {
        flat3Map.put(1, "Value1");
        flat3Map.put(2, "Value2");
        flat3Map.put(null, "Value3");
        String result = flat3Map.get(null);
        assertEquals("Value3", result);
    }

    @Test
    void testGetWithAllKeysNull() {
        flat3Map.put(null, "NullValue1");
        flat3Map.put(null, "NullValue2");
        flat3Map.put(null, "NullValue3");
        String result = flat3Map.get(null);
        assertEquals("NullValue3", result);
    }

    @Test
    void testGetWithMixedKeys() {
        flat3Map.put(1, "Value1");
        flat3Map.put(null, "NullValue");
        flat3Map.put(3, "Value3");
        String result1 = flat3Map.get(1);
        String result2 = flat3Map.get(3);
        String result3 = flat3Map.get(null);
        assertEquals("Value1", result1);
        assertEquals("Value3", result2);
        assertEquals("NullValue", result3);
    }

//     @Test
//     void testGetWithNullKeyWhenDelegateMapIsNotNull() throws Exception {
//         // Given a Flat3Map and a mocked delegateMap
//         Method setDelegateMap = Flat3Map.class.getDeclaredMethod("setDelegateMap", AbstractHashedMap.class);
//         setDelegateMap.setAccessible(true);
//         AbstractHashedMap<Integer, String> mockedDelegateMap = mock(AbstractHashedMap.class);
//         // Set mockedDelegateMap using reflection
//         setDelegateMap.invoke(flat3Map, mockedDelegateMap);
//         when(mockedDelegateMap.get(null)).thenReturn("DelegateNullValue");
//         // When retrieving the value using a null key
//         String result = flat3Map.get(null);
//         // Then it should return the value from the delegateMap
//         assertEquals("DelegateNullValue", result);
//     }

    @Test
    void testGetWithExceededSize() {
        flat3Map.put(1, "Value1");
        flat3Map.put(2, "Value2");
        flat3Map.put(3, "Value3");
        flat3Map.put(4, "Value4");
        String result = flat3Map.get(1);
        assertEquals("Value1", result);
    }

//     @Test
//     void testGetWithDelegateMapThrowsException() throws Exception {
//         // Mock the delegateMap behavior
//         AbstractHashedMap<Integer, String> mockedDelegateMap = mock(AbstractHashedMap.class);
//         when(mockedDelegateMap.get(any())).thenThrow(new RuntimeException("Mocked exception"));
//         // Create Flat3Map and directly set delegateMap
//         flat3Map = new Flat3Map<>();
//         // Directly assign the mocked delegateMap
//         flat3Map.delegateMap = mockedDelegateMap;
//         // Now when we use the get method with a valid key, it should trigger the exception
//         Exception exception = assertThrows(RuntimeException.class, () -> {
//             flat3Map.get(1);
//         });
//         assertEquals("Mocked exception", exception.getMessage());
//     }
@Test
void testGetWithSizeZero() {
    String result = flat3Map.get(1);
    assertNull(result);
}
@Test
void testGetWithSizeOneAndMatchingKey() {
    flat3Map.put(1, "Value1");
    String result = flat3Map.get(1);
    assertEquals("Value1", result);
}
@Test
void testGetWithSizeOneAndNonMatchingKey() {
    flat3Map.put(1, "Value1");
    String result = flat3Map.get(2);
    assertNull(result);
}
@Test
void testGetWithSizeTwoAndFirstKeyMatches() {
    flat3Map.put(1, "Value1");
    flat3Map.put(2, "Value2");
    String result = flat3Map.get(1);
    assertEquals("Value1", result);
}
@Test
void testGetWithSizeTwoAndSecondKeyMatches() {
    flat3Map.put(1, "Value1");
    flat3Map.put(2, "Value2");
    String result = flat3Map.get(2);
    assertEquals("Value2", result);
}
@Test
void testGetWithSizeTwoAndNoKeyMatches() {
    flat3Map.put(1, "Value1");
    flat3Map.put(2, "Value2");
    String result = flat3Map.get(3);
    assertNull(result);
}
@Test
void testGetWithSizeThreeAndFirstKeyMatches() {
    flat3Map.put(1, "Value1");
    flat3Map.put(2, "Value2");
    flat3Map.put(3, "Value3");
    String result = flat3Map.get(1);
    assertEquals("Value1", result);
}
@Test
void testGetWithSizeThreeAndSecondKeyMatches() {
    flat3Map.put(1, "Value1");
    flat3Map.put(2, "Value2");
    flat3Map.put(3, "Value3");
    String result = flat3Map.get(2);
    assertEquals("Value2", result);
}
@Test
void testGetWithSizeThreeAndThirdKeyMatches() {
    flat3Map.put(1, "Value1");
    flat3Map.put(2, "Value2");
    flat3Map.put(3, "Value3");
    String result = flat3Map.get(3);
    assertEquals("Value3", result);
}
@Test
void testGetWithSizeThreeAndNoKeyMatches() {
    flat3Map.put(1, "Value1");
    flat3Map.put(2, "Value2");
    flat3Map.put(3, "Value3");
    String result = flat3Map.get(4);
    assertNull(result);
}
@Test
void testGetWithSizeZeroAndNullKey() {
    String result = flat3Map.get(null);
    assertNull(result);
}
@Test
void testGetWithSizeOneAndNullKeyMatches() {
    flat3Map.put(null, "NullValue");
    String result = flat3Map.get(null);
    assertEquals("NullValue", result);
}
@Test
void testGetWithSizeOneAndNullKeyDoesNotMatch() {
    flat3Map.put(1, "Value1");
    String result = flat3Map.get(null);
    assertNull(result);
}
@Test
void testGetWithSizeTwoAndFirstKeyIsNullAndNullKeyMatches() {
    flat3Map.put(null, "NullValue");
    flat3Map.put(2, "Value2");
    String result = flat3Map.get(null);
    assertEquals("NullValue", result);
}
@Test
void testGetWithSizeTwoAndSecondKeyIsNullAndNullKeyMatches() {
    flat3Map.put(1, "Value1");
    flat3Map.put(null, "NullValue");
    String result = flat3Map.get(null);
    assertEquals("NullValue", result);
}
@Test
void testGetWithSizeTwoAndBothKeysAreNullAndNullKeyMatches() {
    flat3Map.put(null, "NullValue1");
    flat3Map.put(null, "NullValue2");
    String result = flat3Map.get(null);
    assertEquals("NullValue2", result);
}
@Test
void testGetWithSizeThreeAndFirstKeyIsNullAndNullKeyMatches() {
    flat3Map.put(null, "NullValue");
    flat3Map.put(2, "Value2");
    flat3Map.put(3, "Value3");
    String result = flat3Map.get(null);
    assertEquals("NullValue", result);
}
@Test
void testGetWithSizeThreeAndSecondKeyIsNullAndNullKeyMatches() {
    flat3Map.put(1, "Value1");
    flat3Map.put(null, "NullValue");
    flat3Map.put(3, "Value3");
    String result = flat3Map.get(null);
    assertEquals("NullValue", result);
}
@Test
void testGetWithSizeThreeAndThirdKeyIsNullAndNullKeyMatches() {
    flat3Map.put(1, "Value1");
    flat3Map.put(2, "Value2");
    flat3Map.put(null, "NullValue");
    String result = flat3Map.get(null);
    assertEquals("NullValue", result);
}
@Test
void testGetWithSizeThreeAndAllKeysAreNullAndNullKeyMatches() {
    flat3Map.put(null, "NullValue1");
    flat3Map.put(null, "NullValue2");
    flat3Map.put(null, "NullValue3");
    String result = flat3Map.get(null);
    assertEquals("NullValue3", result);
}
@Test
void testGetWithSizeThreeAndNoKeyIsNullAndNullKeyDoesNotMatch() {
    flat3Map.put(1, "Value1");
    flat3Map.put(2, "Value2");
    flat3Map.put(3, "Value3");
    String result = flat3Map.get(null);
    assertNull(result);
}
@Test
void testGetWithSizeThreeAndConditionalBoundary() {
    flat3Map.put(1, "Value1");
    flat3Map.put(2, "Value2");
    flat3Map.put(3, "Value3");
    String result = flat3Map.get(1);
    assertEquals("Value1", result);
}
}