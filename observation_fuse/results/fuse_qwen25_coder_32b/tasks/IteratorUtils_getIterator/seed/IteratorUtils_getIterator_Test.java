package org.apache.commons.collections4;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

// Add other imports if necessary
@Timeout(600)
@ExtendWith(MockitoExtension.class)
class IteratorUtils_getIterator_Test {

    private Iterator<?> resultIterator;

    private Object testObject;

    @BeforeEach
    void setupBeforeEach() {
        // Initialize fields and dependencies that are used in each test case
        // Example test object
        testObject = new ArrayList<>(Arrays.asList("a", "b", "c"));
    }

    @Test
    void testGetIteratorWithCollection() {
        // Testing getIterator with a Collection object
        resultIterator = IteratorUtils.getIterator(testObject);
        assertTrue(resultIterator.hasNext(), "The iterator should have elements.");
        assertEquals("a", resultIterator.next(), "The first element should be 'a'.");
    }

    @Test
    void testGetIteratorWithNull() {
        // Testing getIterator with null input
        resultIterator = IteratorUtils.getIterator(null);
        assertNotNull(resultIterator, "The result iterator should not be null even if input is null.");
        assertFalse(resultIterator.hasNext(), "The iterator should not have any elements.");
    }

    @Test
    void testGetIteratorWithArray() {
        // Testing getIterator with an array input
        Object[] array = new Object[] { "x", "y", "z" };
        resultIterator = IteratorUtils.getIterator(array);
        assertTrue(resultIterator.hasNext(), "The iterator should have elements.");
        assertEquals("x", resultIterator.next(), "The first element should be 'x'.");
    }

    @Test
    void testGetIteratorWithNodeList() {
        // Testing getIterator with an empty NodeList
        NodeList nodeList = new NodeList() {

            // Empty NodeList
            private final Node[] nodes = new Node[0];

            @Override
            public int getLength() {
                return nodes.length;
            }

            @Override
            public Node item(int index) {
                // No node
                return null;
            }
        };
        resultIterator = IteratorUtils.getIterator(nodeList);
        assertNotNull(resultIterator, "The result iterator should not be null.");
        assertFalse(resultIterator.hasNext(), "The iterator should not have any elements.");
    }

    @Test
    void testGetIteratorWithEnumeration() {
        // Testing getIterator with an Enumeration
        Enumeration<String> enumeration = new Enumeration<String>() {

            private final String[] elements = { "foo", "bar" };

            private int index = 0;

            @Override
            public boolean hasMoreElements() {
                return index < elements.length;
            }

            @Override
            public String nextElement() {
                if (hasMoreElements()) {
                    return elements[index++];
                }
                throw new NoSuchElementException();
            }
        };
        resultIterator = IteratorUtils.getIterator(enumeration);
        assertTrue(resultIterator.hasNext(), "The iterator should have elements.");
        assertEquals("foo", resultIterator.next(), "The first element should be 'foo'.");
    }

    @Test
    void testGetIteratorWithMap() {
        // Testing getIterator with a Map
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        resultIterator = IteratorUtils.getIterator(map);
        assertTrue(resultIterator.hasNext(), "The iterator should have elements.");
        assertEquals("value1", resultIterator.next(), "The first value should be 'value1'.");
    }
}
