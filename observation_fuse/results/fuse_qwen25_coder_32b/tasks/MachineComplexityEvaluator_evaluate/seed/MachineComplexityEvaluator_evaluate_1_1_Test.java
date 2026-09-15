package software.amazon.event.ruler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Stack;
import static software.amazon.event.ruler.MatchType.WILDCARD;

@ExtendWith(MockitoExtension.class)
class MachineComplexityEvaluator_evaluate_1_1_Test {

    private MachineComplexityEvaluator evaluator;

    private ByteState mockState;

    @BeforeEach
    void setUp() {
        evaluator = new MachineComplexityEvaluator(10);
        mockState = Mockito.mock(ByteState.class);
    }

    @Test
    void testEvaluateWithNoTransitions() {
        when(mockState.getTransitions()).thenReturn(new HashSet<>());
        int result = evaluator.evaluate(mockState);
        assertEquals(0, result);
    }
}
