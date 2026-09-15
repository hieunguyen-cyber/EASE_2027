package software.amazon.event.ruler;

import software.amazon.event.ruler.ByteMachine;
import software.amazon.event.ruler.Range;
import software.amazon.event.ruler.input.InputCharacter;
import java.lang.reflect.Method;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.core.io.doubleparser.FastDoubleParser;
import software.amazon.event.ruler.input.InputByte;
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

class ByteMachine_addRangePattern_30_1_Test {

    private ByteMachine byteMachine;

    @BeforeEach
    void setUp() {
        byteMachine = new ByteMachine();
    }

    @Test
    void testAddRangePattern_ValidRange() throws Exception {
        // Represents 0001
        byte[] bottom = { '0', '0', '0', '1' };
        // Represents 0005
        byte[] top = { '0', '0', '0', '5' };
        // Closed range [0001, 0005]
        Range range = new Range(bottom, false, top, false, false);
        Method method = ByteMachine.class.getDeclaredMethod("addRangePattern", Range.class);
        method.setAccessible(true);
        Object result = method.invoke(byteMachine, range);
        assertNotNull(result);
    }

    @Test
    void testAddRangePattern_RangeWithOpenBottom() throws Exception {
        // Represents 0001
        byte[] bottom = { '0', '0', '0', '1' };
        // Represents 0005
        byte[] top = { '0', '0', '0', '5' };
        // Open bottom range (0001, 0005]
        Range range = new Range(bottom, true, top, false, false);
        Method method = ByteMachine.class.getDeclaredMethod("addRangePattern", Range.class);
        method.setAccessible(true);
        Object result = method.invoke(byteMachine, range);
        assertNotNull(result);
    }

    @Test
    void testAddRangePattern_InvalidRange() {
        // Represents 0005
        byte[] bottom = { '0', '0', '0', '5' };
        // Represents 0001
        byte[] top = { '0', '0', '0', '1' };
        // Invalid range [0005, 0001]
        Range range = new Range(bottom, false, top, false, false);
        assertThrows(IllegalArgumentException.class, () -> {
            Method method = ByteMachine.class.getDeclaredMethod("addRangePattern", Range.class);
            method.setAccessible(true);
            method.invoke(byteMachine, range);
        });
    }

    @Test
    void testAddRangePattern_RangeWithOpenTop() throws Exception {
        // Represents 0001
        byte[] bottom = { '0', '0', '0', '1' };
        // Represents 0005
        byte[] top = { '0', '0', '0', '5' };
        // Closed bottom, open top [0001, 0005)
        Range range = new Range(bottom, false, top, true, false);
        Method method = ByteMachine.class.getDeclaredMethod("addRangePattern", Range.class);
        method.setAccessible(true);
        Object result = method.invoke(byteMachine, range);
        assertNotNull(result);
    }

    @Test
    void testAddRangePattern_EqualBottomAndTop() {
        // Represents 0001
        byte[] bottom = { '0', '0', '0', '1' };
        // Represents 0001
        byte[] top = { '0', '0', '0', '1' };
        // Invalid range [0001, 0001]
        Range range = new Range(bottom, false, top, false, false);
        assertThrows(IllegalArgumentException.class, () -> {
            Method method = ByteMachine.class.getDeclaredMethod("addRangePattern", Range.class);
            method.setAccessible(true);
            method.invoke(byteMachine, range);
        });
    }
}
