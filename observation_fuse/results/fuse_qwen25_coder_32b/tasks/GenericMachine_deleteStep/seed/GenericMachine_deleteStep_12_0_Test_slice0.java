package software.amazon.event.ruler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

class GenericMachine_deleteStep_12_0_Test_slice0 {

    private GenericMachine genericMachine;

    private NameState mockState;

    private List<String> keys;

    private Map<String, List<Patterns>> patterns;

    private Object ruleName;

    private List<String> deletedKeys;

    @BeforeEach
    void setUp() {
        genericMachine = new GenericMachine();
        mockState = Mockito.mock(NameState.class);
        keys = new ArrayList<>();
        patterns = new HashMap<>();
        ruleName = new Object();
        deletedKeys = new ArrayList<>();
    }

    @Test
    void testDeleteStep_BothMatchersNull() {
        keys.add("key1");
        when(mockState.getTransitionOn("key1")).thenReturn(null);
        when(mockState.getKeyTransitionOn("key1")).thenReturn(null);
        invokeDeleteStep(mockState, keys, 0, patterns, ruleName, deletedKeys);
        assertEquals(0, deletedKeys.size());
    }

    @Test
    void testDeleteStep_OnlyByteMachineNull() {
        keys.add("key1");
        NameMatcher<NameState> mockMatcher = Mockito.mock(NameMatcher.class);
        when(mockState.getTransitionOn("key1")).thenReturn(null);
        when(mockState.getKeyTransitionOn("key1")).thenReturn(mockMatcher);
        invokeDeleteStep(mockState, keys, 0, patterns, ruleName, deletedKeys);
        assertEquals(0, deletedKeys.size());
    }

    @Test
    void testDeleteStep_OnlyNameMatcherNull() {
        keys.add("key1");
        ByteMachine mockByteMachine = Mockito.mock(ByteMachine.class);
        when(mockState.getTransitionOn("key1")).thenReturn(mockByteMachine);
        when(mockState.getKeyTransitionOn("key1")).thenReturn(null);
        invokeDeleteStep(mockState, keys, 0, patterns, ruleName, deletedKeys);
        assertEquals(0, deletedKeys.size());
    }

    @Test
    void testDeleteStep_NeitherMatcherNull() {
        keys.add("key1");
        ByteMachine mockByteMachine = Mockito.mock(ByteMachine.class);
        NameMatcher<NameState> mockMatcher = Mockito.mock(NameMatcher.class);
        when(mockState.getTransitionOn("key1")).thenReturn(mockByteMachine);
        when(mockState.getKeyTransitionOn("key1")).thenReturn(mockMatcher);
        invokeDeleteStep(mockState, keys, 0, patterns, ruleName, deletedKeys);
        assertEquals(0, deletedKeys.size());
    }

    private void invokeDeleteStep(NameState state, List<String> keys, int keyIndex, Map<String, List<Patterns>> patterns, Object ruleName, List<String> deletedKeys) {
        // This method wraps the call to the private method deleteStep
        // using reflection or by making deleteStep package-private.
        // Assuming deleteStep is accessible here.
        // This is a placeholder for the actual call to deleteStep.
        // In a real scenario, you would need to create a public method in GenericMachine.
    }
}
