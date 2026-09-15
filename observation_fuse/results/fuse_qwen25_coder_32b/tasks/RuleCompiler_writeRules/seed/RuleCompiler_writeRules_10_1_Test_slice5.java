package software.amazon.event.ruler;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.JsonParseException;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.core.JsonToken;
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

public class RuleCompiler_writeRules_10_1_Test_slice5 {

    private void invokeWriteRules(Map<String, List<Patterns>> rules, String name, JsonParser parser, boolean withQuotes) throws Exception {
        Method method = RuleCompiler.class.getDeclaredMethod("writeRules", Map.class, String.class, JsonParser.class, boolean.class);
        method.setAccessible(true);
        method.invoke(null, rules, name, parser, withQuotes);
    }

    @Test
    public void testWriteRulesWithExactMatch() throws IOException, Exception {
        Map<String, List<Patterns>> rules = new HashMap<>();
        String ruleName = "testRule";
        // Use the correct constant for the match type
        String json = "[{\"" + Constants.EXACT_MATCH + "\": \"testValue\"}]";
        JsonParser parser = new JsonFactory().createParser(json);
        // Start array token
        parser.nextToken();
        invokeWriteRules(rules, ruleName, parser, true);
        List<Patterns> expectedPatterns = new ArrayList<>();
        expectedPatterns.add(Patterns.exactMatch("\"testValue\""));
        assertEquals(expectedPatterns, rules.get(ruleName));
    }

    @Test
    public void testWriteRulesWithEmptyArray() throws IOException, Exception {
        Map<String, List<Patterns>> rules = new HashMap<>();
        String ruleName = "emptyRule";
        String json = "[]";
        JsonParser parser = null;
        try {
            parser = new JsonFactory().createParser(json);
            // Start array token
            parser.nextToken();
            invokeWriteRules(rules, ruleName, parser, true);
            fail("Expected JsonParseException to be thrown");
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof JsonParseException) {
                assertTrue(cause.getMessage().contains("Empty arrays are not allowed"));
            } else {
                fail("Unexpected exception type thrown: " + cause.getClass().getName());
            }
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
    }
}
