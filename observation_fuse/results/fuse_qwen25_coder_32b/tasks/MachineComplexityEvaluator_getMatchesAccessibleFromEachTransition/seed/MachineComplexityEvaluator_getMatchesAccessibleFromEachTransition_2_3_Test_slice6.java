package software.amazon.event.ruler;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import static software.amazon.event.ruler.MatchType.WILDCARD;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class MachineComplexityEvaluator_getMatchesAccessibleFromEachTransition_2_3_Test_slice6 {

    private MachineComplexityEvaluator evaluator;

    private ByteState byteState;

    @BeforeEach
    void setUp() {
        evaluator = new MachineComplexityEvaluator(1);
        byteState = new ByteState();
    }

    @Test
    void testGetMatchesAccessibleFromEachTransition() throws Exception {
        SingleByteTransition transition = new SingleByteTransition() {

            @Override
            ByteMatch getMatch() {
                return new ByteMatch(new Patterns(MatchType.WILDCARD), new NameState());
            }

            @Override
            SingleByteTransition setMatch(ByteMatch match) {
                return this;
            }

            @Override
            ByteTransition getTransitionForAllBytes() {
                return null;
            }

            @Override
            ByteState getNextByteState() {
                return byteState;
            }

            @Override
            Set<ShortcutTransition> getShortcuts() {
                return new HashSet<>();
            }

            @Override
            Set<ByteTransition> getTransitions() {
                return new HashSet<>();
            }

            @Override
            ByteTransition getTransition(byte b) {
                return null;
            }

            @Override
            SingleByteTransition setNextByteState(ByteState nextState) {
                // Correctly implement the method
                return this;
            }
        };
        byteState.addTransition((byte) 1, transition);
        Method method = MachineComplexityEvaluator.class.getDeclaredMethod("getMatchesAccessibleFromEachTransition", ByteState.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<SingleByteTransition, Set<ByteMatch>> result = (Map<SingleByteTransition, Set<ByteMatch>>) method.invoke(evaluator, byteState);
        assertNotNull(result);
        assertTrue(result.containsKey(transition));
        assertFalse(result.get(transition).isEmpty());
    }
}
