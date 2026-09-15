package software.amazon.event.ruler;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.core.JsonParseException;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public class RuleCompiler_writeRules_10_2_Test_slice6 {

    private Map<String, List<Patterns>> ruleMap;

    private JsonParser mockParser;

    @BeforeEach
    public void setUp() throws IOException {
        ruleMap = new HashMap<>();
        mockParser = Mockito.mock(JsonParser.class);
    }

    private void invokeWriteRules(Map<String, List<Patterns>> ruleMap, String name, JsonParser parser, boolean withQuotes) throws Exception {
        Method method = RuleCompiler.class.getDeclaredMethod("writeRules", Map.class, String.class, JsonParser.class, boolean.class);
        method.setAccessible(true);
        method.invoke(null, ruleMap, name, parser, withQuotes);
    }

    @Test
    public void testWriteRules_EmptyArray() throws Exception {
        when(mockParser.nextToken()).thenReturn(JsonToken.END_ARRAY);
        Exception exception = assertThrows(JsonParseException.class, () -> {
            invokeWriteRules(ruleMap, "testRule", mockParser, false);
        });
        assertEquals("Empty arrays are not allowed", exception.getMessage());
    }

    @Test
    public void testWriteRules_SingleStringValue() throws Exception {
        when(mockParser.nextToken()).thenReturn(JsonToken.VALUE_STRING).thenReturn(JsonToken.END_ARRAY);
        when(mockParser.getText()).thenReturn("testValue");
        invokeWriteRules(ruleMap, "testRule", mockParser, false);
        assertEquals(1, ruleMap.get("testRule").size());
        assertTrue(ruleMap.get("testRule").get(0) instanceof ValuePatterns);
    }

    @Test
    public void testWriteRules_SingleNumberValue() throws Exception {
        when(mockParser.nextToken()).thenReturn(JsonToken.VALUE_NUMBER_FLOAT).thenReturn(JsonToken.END_ARRAY);
        when(mockParser.getDoubleValue()).thenReturn(42.0);
        when(mockParser.getText()).thenReturn("42");
        invokeWriteRules(ruleMap, "testRule", mockParser, false);
        assertEquals(2, ruleMap.get("testRule").size());
        assertTrue(ruleMap.get("testRule").get(0) instanceof ValuePatterns);
        assertTrue(ruleMap.get("testRule").get(1) instanceof ValuePatterns);
    }

    @Test
    public void testWriteRules_NullValue() throws Exception {
        when(mockParser.nextToken()).thenReturn(JsonToken.VALUE_NULL).thenReturn(JsonToken.END_ARRAY);
        invokeWriteRules(ruleMap, "testRule", mockParser, false);
        assertEquals(1, ruleMap.get("testRule").size());
        assertTrue(ruleMap.get("testRule").get(0) instanceof ValuePatterns);
    }

    @Test
    public void testWriteRules_BooleanTrueValue() throws Exception {
        when(mockParser.nextToken()).thenReturn(JsonToken.VALUE_TRUE).thenReturn(JsonToken.END_ARRAY);
        invokeWriteRules(ruleMap, "testRule", mockParser, false);
        assertEquals(1, ruleMap.get("testRule").size());
        assertTrue(ruleMap.get("testRule").get(0) instanceof ValuePatterns);
    }

    @Test
    public void testWriteRules_BooleanFalseValue() throws Exception {
        when(mockParser.nextToken()).thenReturn(JsonToken.VALUE_FALSE).thenReturn(JsonToken.END_ARRAY);
        invokeWriteRules(ruleMap, "testRule", mockParser, false);
        assertEquals(1, ruleMap.get("testRule").size());
    }
}
