package software.amazon.event.ruler;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.concurrent.ThreadSafe;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.core.io.doubleparser.FastDoubleParser;
import software.amazon.event.ruler.input.InputByte;
import software.amazon.event.ruler.input.InputCharacter;
import software.amazon.event.ruler.input.InputCharacterType;
import software.amazon.event.ruler.input.InputMultiByteSet;
import software.amazon.event.ruler.input.MultiByte;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import static software.amazon.event.ruler.CompoundByteTransition.coalesce;
import static software.amazon.event.ruler.MatchType.EXACT;
import static software.amazon.event.ruler.MatchType.EXISTS;
import static software.amazon.event.ruler.MatchType.SUFFIX;
import static software.amazon.event.ruler.MatchType.ANYTHING_BUT_SUFFIX;
import static software.amazon.event.ruler.input.MultiByte.MAX_FIRST_BYTE_FOR_ONE_BYTE_CHAR;
import static software.amazon.event.ruler.input.MultiByte.MAX_FIRST_BYTE_FOR_TWO_BYTE_CHAR;
import static software.amazon.event.ruler.input.MultiByte.MAX_NON_FIRST_BYTE;
import static software.amazon.event.ruler.input.MultiByte.MIN_FIRST_BYTE_FOR_ONE_BYTE_CHAR;
import static software.amazon.event.ruler.input.MultiByte.MIN_FIRST_BYTE_FOR_TWO_BYTE_CHAR;
import static software.amazon.event.ruler.input.DefaultParser.getParser;

@ExtendWith(MockitoExtension.class)
class ByteMachine_updateMatchReferences_44_0_Test_slice4 {

    private ByteMachine byteMachine;

    private ByteMatch mockByteMatch;

    @BeforeEach
    void setUp() {
        byteMachine = new ByteMachine();
        mockByteMatch = mock(ByteMatch.class);
    }

    @Test
    void testUpdateMatchReferences_ExactPattern() {
        when(mockByteMatch.getPattern()).thenReturn(new Patterns(MatchType.EXACT));
        invokeUpdateMatchReferences(mockByteMatch);
        assertEquals(0, getField(byteMachine, "hasNumeric").get());
        assertEquals(0, getField(byteMachine, "hasIP").get());
        assertEquals(0, getField(byteMachine, "hasSuffix").get());
    }

    @Test
    void testUpdateMatchReferences_SuffixPattern() {
        getField(byteMachine, "hasSuffix").set(1);
        when(mockByteMatch.getPattern()).thenReturn(new Patterns(MatchType.SUFFIX));
        invokeUpdateMatchReferences(mockByteMatch);
        assertEquals(0, getField(byteMachine, "hasSuffix").get());
    }

    @Test
    void testUpdateMatchReferences_NumericEqPattern() {
        getField(byteMachine, "hasNumeric").set(1);
        when(mockByteMatch.getPattern()).thenReturn(new Patterns(MatchType.NUMERIC_EQ));
        invokeUpdateMatchReferences(mockByteMatch);
        assertEquals(0, getField(byteMachine, "hasNumeric").get());
    }

    private void invokeUpdateMatchReferences(ByteMatch match) {
        try {
            java.lang.reflect.Method method = ByteMachine.class.getDeclaredMethod("updateMatchReferences", ByteMatch.class);
            method.setAccessible(true);
            method.invoke(byteMachine, match);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AtomicInteger getField(ByteMachine byteMachine, String fieldName) {
        try {
            java.lang.reflect.Field field = ByteMachine.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (AtomicInteger) field.get(byteMachine);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
