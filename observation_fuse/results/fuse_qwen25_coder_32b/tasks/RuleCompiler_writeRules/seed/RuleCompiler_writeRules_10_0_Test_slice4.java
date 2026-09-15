package software.amazon.event.ruler;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Method;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.core.JsonParseException;

public class RuleCompiler_writeRules_10_0_Test_slice4 {

    private JsonFactory jsonFactory;

    private JsonParser parser;

    @BeforeEach
    public void setUp() throws IOException {
        jsonFactory = new JsonFactory();
    }

    private void invokeWriteRules(Map<String, List<Patterns>> rule, String name, JsonParser parser, boolean withQuotes) throws Exception {
        Method method = RuleCompiler.class.getDeclaredMethod("writeRules", Map.class, String.class, JsonParser.class, boolean.class);
        method.setAccessible(true);
        method.invoke(null, rule, name, parser, withQuotes);
    }

    @Test
    public void testWriteRules_EmptyArray() throws Exception {
        Map<String, List<Patterns>> rule = new HashMap<>();
        parser = jsonFactory.createParser(new StringReader("[]"));
        Exception thrown = assertThrows(Exception.class, () -> {
            invokeWriteRules(rule, "testRule", parser, false);
        });
        assertTrue(thrown.getCause() instanceof JsonParseException);
        assertEquals("Empty arrays are not allowed", thrown.getCause().getMessage());
    }

    @Test
    public void testWriteRules_SingleStringValue() throws Exception {
        Map<String, List<Patterns>> rule = new HashMap<>();
        parser = jsonFactory.createParser(new StringReader("[\"testString\"]"));
        // Move to START_ARRAY
        parser.nextToken();
        invokeWriteRules(rule, "testRule", parser, false);
        assertEquals(1, rule.get("testRule").size());
        assertTrue(rule.get("testRule").get(0) instanceof ValuePatterns);
        assertEquals(Patterns.exactMatch("testString"), rule.get("testRule").get(0));
    }

    @Test
    public void testWriteRules_NumericValue() throws Exception {
        Map<String, List<Patterns>> rule = new HashMap<>();
        parser = jsonFactory.createParser(new StringReader("[123.45]"));
        // Move to START_ARRAY
        parser.nextToken();
        invokeWriteRules(rule, "testRule", parser, false);
        assertEquals(2, rule.get("testRule").size());
        assertTrue(rule.get("testRule").get(0) instanceof ValuePatterns);
        assertEquals(Patterns.numericEquals(123.45), rule.get("testRule").get(0));
        assertEquals(Patterns.exactMatch("123.45"), rule.get("testRule").get(1));
    }
}
