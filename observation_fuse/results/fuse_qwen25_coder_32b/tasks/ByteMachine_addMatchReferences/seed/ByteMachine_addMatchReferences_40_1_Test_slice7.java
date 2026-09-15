package software.amazon.event.ruler;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
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

class ByteMachine_addMatchReferences_40_1_Test_slice7 {

    private ByteMachine byteMachine;

    @BeforeEach
    void setUp() {
        byteMachine = new ByteMachine();
    }

    private int getHasNumeric() throws Exception {
        Field field = ByteMachine.class.getDeclaredField("hasNumeric");
        field.setAccessible(true);
        return ((AtomicInteger) field.get(byteMachine)).get();
    }

    private int getHasIP() throws Exception {
        Field field = ByteMachine.class.getDeclaredField("hasIP");
        field.setAccessible(true);
        return ((AtomicInteger) field.get(byteMachine)).get();
    }

    private int getHasSuffix() throws Exception {
        Field field = ByteMachine.class.getDeclaredField("hasSuffix");
        field.setAccessible(true);
        return ((AtomicInteger) field.get(byteMachine)).get();
    }

    private void invokeAddMatchReferences(ByteMatch byteMatch) throws Exception {
        Method method = ByteMachine.class.getDeclaredMethod("addMatchReferences", ByteMatch.class);
        method.setAccessible(true);
        method.invoke(byteMachine, byteMatch);
    }

    @Test
    void testAddMatchReferencesWithExactPattern() throws Exception {
        NameState nextNameState = new NameState();
        Patterns patterns = new Patterns(MatchType.EXACT);
        ByteMatch byteMatch = new ByteMatch(patterns, nextNameState);
        invokeAddMatchReferences(byteMatch);
        assertEquals(0, getHasNumeric());
        assertEquals(0, getHasIP());
        assertEquals(0, getHasSuffix());
    }

    @Test
    void testAddMatchReferencesWithSuffixPattern() throws Exception {
        NameState nextNameState = new NameState();
        Patterns patterns = new Patterns(MatchType.SUFFIX);
        ByteMatch byteMatch = new ByteMatch(patterns, nextNameState);
        invokeAddMatchReferences(byteMatch);
        assertEquals(0, getHasNumeric());
        assertEquals(0, getHasIP());
        assertEquals(1, getHasSuffix());
    }

    @Test
    void testAddMatchReferencesWithNumericPattern() throws Exception {
        NameState nextNameState = new NameState();
        Patterns patterns = new Patterns(MatchType.NUMERIC_EQ);
        ByteMatch byteMatch = new ByteMatch(patterns, nextNameState);
        invokeAddMatchReferences(byteMatch);
        assertEquals(1, getHasNumeric());
        assertEquals(0, getHasIP());
        assertEquals(0, getHasSuffix());
    }
}
