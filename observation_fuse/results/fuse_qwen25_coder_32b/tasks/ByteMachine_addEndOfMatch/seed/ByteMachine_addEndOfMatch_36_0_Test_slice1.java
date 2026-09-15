package software.amazon.event.ruler;

import software.amazon.event.ruler.input.InputCharacter;
import software.amazon.event.ruler.input.InputByte;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import software.amazon.event.ruler.input.InputCharacterType;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.core.io.doubleparser.FastDoubleParser;
import software.amazon.event.ruler.input.InputMultiByteSet;
import software.amazon.event.ruler.input.MultiByte;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.concurrent.ThreadSafe;
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

class ByteMachine_addEndOfMatch_36_0_Test_slice1 {

    private ByteMachine byteMachine;

    @BeforeEach
    void setUp() {
        byteMachine = new ByteMachine();
    }

    @Test
    void testAddEndOfMatch_WithWildcardCharacter() throws Exception {
        // Arrange
        ByteState state = new ByteState();
        ByteState prevState = new ByteState();
        InputCharacter[] characters = new InputCharacter[] { createMockInputByte('*') };
        Patterns pattern = new Patterns(MatchType.WILDCARD);
        NameState nameStateCandidate = null;
        // Act
        NameState result = invokeAddEndOfMatch(state, prevState, characters, 0, pattern, nameStateCandidate);
        // Assert
        assertNotNull(result, "NameState should not be null");
        // Accessing the private field startStateMatch using reflection
        Field startStateMatchField = ByteMachine.class.getDeclaredField("startStateMatch");
        startStateMatchField.setAccessible(true);
        ByteMatch startStateMatch = (ByteMatch) startStateMatchField.get(byteMachine);
        assertNotNull(startStateMatch, "Start state match should not be null");
        assertEquals(pattern, startStateMatch.getPattern(), "Pattern should match the input pattern");
        assertEquals(result, startStateMatch.getNextNameState(), "Next NameState should match the result");
    }

    private InputCharacter createMockInputByte(char value) {
        InputByte mockInputByte = mock(InputByte.class);
        Mockito.when(mockInputByte.getType()).thenReturn(InputCharacterType.WILDCARD);
        return mockInputByte;
    }

    private NameState invokeAddEndOfMatch(ByteState state, ByteState prevState, InputCharacter[] characters, int charIndex, Patterns pattern, NameState nameStateCandidate) throws Exception {
        Method method = ByteMachine.class.getDeclaredMethod("addEndOfMatch", ByteState.class, ByteState.class, InputCharacter[].class, int.class, Patterns.class, NameState.class);
        method.setAccessible(true);
        return (NameState) method.invoke(byteMachine, state, prevState, characters, charIndex, pattern, nameStateCandidate);
    }
}
