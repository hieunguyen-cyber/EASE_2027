package software.amazon.event.ruler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

class ByteMachine_addMatchReferences_40_2_Test_slice10 {

    private ByteMachine byteMachine;

    @BeforeEach
    void setUp() {
        byteMachine = new ByteMachine();
    }

    private int getHasSuffixCount() throws Exception {
        Field field = ByteMachine.class.getDeclaredField("hasSuffix");
        field.setAccessible(true);
        AtomicInteger hasSuffix = (AtomicInteger) field.get(byteMachine);
        return hasSuffix.get();
    }

    private int getHasNumericCount() throws Exception {
        Field field = ByteMachine.class.getDeclaredField("hasNumeric");
        field.setAccessible(true);
        AtomicInteger hasNumeric = (AtomicInteger) field.get(byteMachine);
        return hasNumeric.get();
    }

    @Test
    void testAddMatchReferences_withExactMatch() throws Exception {
        Patterns patterns = new Patterns(MatchType.EXACT);
        NameState nextNameState = Mockito.mock(NameState.class);
        ByteMatch match = new ByteMatch(patterns, nextNameState);
        Method method = ByteMachine.class.getDeclaredMethod("addMatchReferences", ByteMatch.class);
        method.setAccessible(true);
        method.invoke(byteMachine, match);
        assertEquals(0, getHasSuffixCount());
        assertEquals(0, getHasNumericCount());
    }

    @Test
    void testAddMatchReferences_withSuffixMatch() throws Exception {
        Patterns patterns = new Patterns(MatchType.SUFFIX);
        NameState nextNameState = Mockito.mock(NameState.class);
        ByteMatch match = new ByteMatch(patterns, nextNameState);
        Method method = ByteMachine.class.getDeclaredMethod("addMatchReferences", ByteMatch.class);
        method.setAccessible(true);
        method.invoke(byteMachine, match);
        assertEquals(1, getHasSuffixCount());
        assertEquals(0, getHasNumericCount());
    }

    @Test
    void testAddMatchReferences_withNumericMatch() throws Exception {
        Patterns patterns = new Patterns(MatchType.NUMERIC_EQ);
        NameState nextNameState = Mockito.mock(NameState.class);
        ByteMatch match = new ByteMatch(patterns, nextNameState);
        Method method = ByteMachine.class.getDeclaredMethod("addMatchReferences", ByteMatch.class);
        method.setAccessible(true);
        method.invoke(byteMachine, match);
        assertEquals(0, getHasSuffixCount());
        assertEquals(1, getHasNumericCount());
    }
}
