package software.amazon.event.ruler;

import software.amazon.event.ruler.input.InputCharacter;
import software.amazon.event.ruler.input.InputCharacterType;
import software.amazon.event.ruler.input.InputByte;
import software.amazon.event.ruler.input.InputMultiByteSet;
import java.lang.reflect.Method;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.core.io.doubleparser.FastDoubleParser;
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

class ByteMachine_findMatchPattern_28_0_Test_slice1 {

    private ByteMachine byteMachine;

    @BeforeEach
    void setUp() {
        byteMachine = new ByteMachine();
    }

    private NameState invokeFindMatchPattern(InputCharacter[] inputCharacters, Patterns pattern) throws Exception {
        Method method = ByteMachine.class.getDeclaredMethod("findMatchPattern", InputCharacter[].class, Patterns.class);
        method.setAccessible(true);
        return (NameState) method.invoke(byteMachine, (Object) inputCharacters, pattern);
    }

    @Test
    void testFindMatchPattern_withValidInput_shouldReturnNextNameState() throws Exception {
        // Arrange
        InputCharacter inputByte1 = Mockito.mock(InputByte.class);
        InputCharacter inputByte2 = Mockito.mock(InputByte.class);
        when(inputByte1.getType()).thenReturn(InputCharacterType.BYTE);
        when(inputByte2.getType()).thenReturn(InputCharacterType.BYTE);
        InputCharacter[] inputCharacters = new InputCharacter[] { inputByte1, inputByte2 };
        Patterns pattern = new Patterns(MatchType.EXACT);
        // Act
        NameState result = invokeFindMatchPattern(inputCharacters, pattern);
        // Assert
        assertNotNull(result);
    }

    @Test
    void testFindMatchPattern_withNoTransitions_shouldReturnNull() throws Exception {
        // Arrange
        InputCharacter inputByte = Mockito.mock(InputByte.class);
        when(inputByte.getType()).thenReturn(InputCharacterType.BYTE);
        InputCharacter[] inputCharacters = new InputCharacter[] { inputByte };
        Patterns pattern = new Patterns(MatchType.EXACT);
        // Act
        NameState result = invokeFindMatchPattern(inputCharacters, pattern);
        // Assert
        assertNull(result);
    }

    @Test
    void testFindMatchPattern_withWildcardCharacter_shouldReturnNextNameState() throws Exception {
        // Arrange
        InputMultiByteSet inputMultiByteSet = Mockito.mock(InputMultiByteSet.class);
        when(inputMultiByteSet.getType()).thenReturn(InputCharacterType.WILDCARD);
        InputCharacter[] inputCharacters = new InputCharacter[] { inputMultiByteSet, Mockito.mock(InputByte.class) };
        Patterns pattern = new Patterns(MatchType.EXISTS);
        // Act
        NameState result = invokeFindMatchPattern(inputCharacters, pattern);
        // Assert
        assertNotNull(result);
    }

    @Test
    void testFindMatchPattern_withNonMatchingPattern_shouldReturnNull() throws Exception {
        // Arrange
        InputCharacter inputByte1 = Mockito.mock(InputByte.class);
        InputCharacter inputByte2 = Mockito.mock(InputByte.class);
        when(inputByte1.getType()).thenReturn(InputCharacterType.BYTE);
        when(inputByte2.getType()).thenReturn(InputCharacterType.BYTE);
        InputCharacter[] inputCharacters = new InputCharacter[] { inputByte1, inputByte2 };
        Patterns pattern = new Patterns(MatchType.SUFFIX);
        // Act
        NameState result = invokeFindMatchPattern(inputCharacters, pattern);
        // Assert
        assertNull(result);
    }
}
