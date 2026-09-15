package software.amazon.event.ruler;

import software.amazon.event.ruler.input.InputCharacter;
import software.amazon.event.ruler.input.InputCharacterType;
import software.amazon.event.ruler.input.InputByte;
import software.amazon.event.ruler.Patterns;
import java.lang.reflect.Method;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
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

class ByteMachine_findMatchPattern_28_2_Test_slice12 {

    private final ByteMachine byteMachine = new ByteMachine();

    @Test
    void testFindMatchPatternExactMatch() throws Exception {
        InputCharacter[] inputBytes = new InputCharacter[] { createInputByte((byte) 'a'), createInputByte((byte) 'b') };
        Patterns pattern = new Patterns(MatchType.EXACT);
        NameState result = invokeFindMatchPattern(inputBytes, pattern);
        assertNotNull(result);
    }

    @Test
    void testFindMatchPatternNoMatch() throws Exception {
        InputCharacter[] inputBytes = new InputCharacter[] { createInputByte((byte) 'x'), createInputByte((byte) 'y') };
        Patterns pattern = new Patterns(MatchType.EXACT);
        NameState result = invokeFindMatchPattern(inputBytes, pattern);
        assertNull(result);
    }

    private InputCharacter createInputByte(byte value) {
        InputByte mockInputByte = Mockito.mock(InputByte.class);
        Mockito.when(mockInputByte.getType()).thenReturn(InputCharacterType.BYTE);
        return mockInputByte;
    }

    private NameState invokeFindMatchPattern(InputCharacter[] inputBytes, Patterns pattern) throws Exception {
        Method method = ByteMachine.class.getDeclaredMethod("findMatchPattern", InputCharacter[].class, Patterns.class);
        method.setAccessible(true);
        return (NameState) method.invoke(byteMachine, (Object) inputBytes, pattern);
    }
}
