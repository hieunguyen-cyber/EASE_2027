package org.apache.commons.collections4.trie;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.Map;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class AbstractPatriciaTrie_removeInternalEntry_Test {

    private AbstractPatriciaTrie<String, String> trie;

    private AbstractPatriciaTrie.TrieEntry<String, String> entry;

    // Concrete subclass for testing
    static class ConcretePatriciaTrie<K, V> extends AbstractPatriciaTrie<K, V> {

        ConcretePatriciaTrie(KeyAnalyzer<? super K> keyAnalyzer, Map<K, V> map) {
            super(keyAnalyzer, map);
        }
        // Other necessary method implementations must be provided
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialize the trie with a mocked KeyAnalyzer and an empty map
        KeyAnalyzer<String> keyAnalyzer = mock(KeyAnalyzer.class);
        trie = new ConcretePatriciaTrie<>(keyAnalyzer, new HashMap<String, String>());
        // Initialize some entries
        AbstractPatriciaTrie.TrieEntry<String, String> entry1 = new AbstractPatriciaTrie.TrieEntry<>("key1", "value1", 0);
        AbstractPatriciaTrie.TrieEntry<String, String> entry2 = new AbstractPatriciaTrie.TrieEntry<>("key2", "value2", 1);
        trie.addEntry(entry1, 0);
        trie.addEntry(entry2, 1);
    }

    // Reflection helper method to invoke the private removeInternalEntry method
    private void invokeRemoveInternalEntry(AbstractPatriciaTrie<String, String> trie, AbstractPatriciaTrie.TrieEntry<String, String> entry) throws Exception {
        var method = AbstractPatriciaTrie.class.getDeclaredMethod("removeInternalEntry", AbstractPatriciaTrie.TrieEntry.class);
        method.setAccessible(true);
        method.invoke(trie, entry);
    }

    @Test
    void testRemoveInternalEntry_throwsIllegalArgumentException_whenRootEntryIsDeleted() {
        // Given
        AbstractPatriciaTrie.TrieEntry<String, String> rootEntry = new AbstractPatriciaTrie.TrieEntry<>(null, null, -1);
        // Assume root entry is the one we're working with
        // Use reflection to access the private root field
        try {
            var rootField = AbstractPatriciaTrie.class.getDeclaredField("root");
            rootField.setAccessible(true);
            rootField.set(trie, rootEntry);
        } catch (Exception e) {
            fail("Failed to set root entry: " + e.getMessage());
        }
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            invokeRemoveInternalEntry(trie, rootEntry);
        });
    }

    @Test
    void testRemoveInternalEntry_throwsIllegalArgumentException_whenEntryIsNotInternal() {
        // Given
        // Entry needs to be not internal for this case to hold true
        // Creating an external node
        entry = new AbstractPatriciaTrie.TrieEntry<>(null, null, -1);
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            invokeRemoveInternalEntry(trie, entry);
        });
    }

    @Test
    void testRemoveInternalEntry_removesInternalNode() throws Exception {
        // Given
        // Setup an internal TrieEntry with parent and predecessor properly set
        AbstractPatriciaTrie.TrieEntry<String, String> parent = new AbstractPatriciaTrie.TrieEntry<>("parentKey", "parentValue", 1);
        AbstractPatriciaTrie.TrieEntry<String, String> internalEntry = new AbstractPatriciaTrie.TrieEntry<>("internalKey", "internalValue", 0);
        internalEntry.parent = parent;
        // self-uplinking
        internalEntry.predecessor = internalEntry;
        // Link parent to this internal entry in the trie
        trie.addEntry(parent, 0);
        trie.addEntry(internalEntry, 0);
        // When
        invokeRemoveInternalEntry(trie, internalEntry);
        // Then - Validate internalEntry is removed and the parent's links are correct
        // More assertions can be added here to verify the state of trie
    }

    @Test
    void testRemoveInternalEntry_removesCorrectly() throws Exception {
        // Given
        AbstractPatriciaTrie.TrieEntry<String, String> parent = new AbstractPatriciaTrie.TrieEntry<>("parent", "value", 1);
        AbstractPatriciaTrie.TrieEntry<String, String> internalEntry = new AbstractPatriciaTrie.TrieEntry<>("internal", "value", 0);
        internalEntry.parent = parent;
        // self-uplinking for this case
        internalEntry.predecessor = internalEntry;
        parent.left = internalEntry;
        trie.addEntry(parent, 1);
        trie.addEntry(internalEntry, 0);
        // When
        invokeRemoveInternalEntry(trie, internalEntry);
        // Then - Verify the parent links have been updated correctly
        assertEquals(null, parent.left);
        assertEquals(null, internalEntry.parent);
        // More assertions can be added to verify the correctness of the internal state
    }

    @Test
    void testRemoveInternalEntry_failsWhenChildLinksAreBroken() throws Exception {
        // Given
        AbstractPatriciaTrie.TrieEntry<String, String> parent = new AbstractPatriciaTrie.TrieEntry<>("parent", "value", 1);
        AbstractPatriciaTrie.TrieEntry<String, String> internalEntry = new AbstractPatriciaTrie.TrieEntry<>("internal", "value", 0);
        AbstractPatriciaTrie.TrieEntry<String, String> child = new AbstractPatriciaTrie.TrieEntry<>("child", "value", 2);
        internalEntry.left = child;
        internalEntry.parent = parent;
        parent.left = internalEntry;
        trie.addEntry(parent, 1);
        trie.addEntry(internalEntry, 0);
        trie.addEntry(child, 2);
        // When
        invokeRemoveInternalEntry(trie, internalEntry);
        // Then - Child should now be re-parented correctly
        assertEquals(parent, child.parent);
    }

    @Test
    void testRemoveInternalEntry_failsWhenEntryIsNotInternal() {
        // Given
        // External entry
        AbstractPatriciaTrie.TrieEntry<String, String> entry = new AbstractPatriciaTrie.TrieEntry<>(null, null, -1);
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            invokeRemoveInternalEntry(trie, entry);
        });
    }
}
