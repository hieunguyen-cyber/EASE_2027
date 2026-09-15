package software.amazon.event.ruler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

class Ruler_matches_4_3_Test_slice1 {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Method matchesMethod;

    @BeforeEach
    void setUp() throws Exception {
        matchesMethod = Ruler.class.getDeclaredMethod("matches", JsonNode.class, Patterns.class);
        // Allow access to the private method
        matchesMethod.setAccessible(true);
    }

    private boolean invokeMatches(JsonNode jsonNode, Patterns pattern) throws Exception {
        return (Boolean) matchesMethod.invoke(null, jsonNode, pattern);
    }

    @Test
    void testMatchesExactString() throws Exception {
        JsonNode jsonNode = objectMapper.createObjectNode().put("key", "value");
        Patterns pattern = new ValuePatterns(MatchType.EXACT, "value");
        assertTrue(invokeMatches(jsonNode.get("key"), pattern));
    }

    @Test
    void testMatchesExactStringWithQuotes() throws Exception {
        JsonNode jsonNode = objectMapper.createObjectNode().put("key", "value");
        Patterns pattern = new ValuePatterns(MatchType.EXACT, "\"value\"");
        assertTrue(invokeMatches(jsonNode.get("key"), pattern));
    }

    @Test
    void testDoesNotMatchExactString() throws Exception {
        JsonNode jsonNode = objectMapper.createObjectNode().put("key", "value");
        Patterns pattern = new ValuePatterns(MatchType.EXACT, "different");
        assertFalse(invokeMatches(jsonNode.get("key"), pattern));
    }

    @Test
    void testMatchesExactNumber() throws Exception {
        JsonNode jsonNode = objectMapper.createObjectNode().put("key", 123);
        Patterns pattern = new ValuePatterns(MatchType.EXACT, ComparableNumber.generate(123));
        assertTrue(invokeMatches(jsonNode.get("key"), pattern));
    }

    @Test
    void testDoesNotMatchExactNumber() throws Exception {
        JsonNode jsonNode = objectMapper.createObjectNode().put("key", 123);
        Patterns pattern = new ValuePatterns(MatchType.EXACT, ComparableNumber.generate(456));
        assertFalse(invokeMatches(jsonNode.get("key"), pattern));
    }

    @Test
    void testMatchesExactNull() throws Exception {
        JsonNode jsonNode = objectMapper.createObjectNode().putNull("key");
        Patterns pattern = new ValuePatterns(MatchType.EXACT, "null");
        assertFalse(invokeMatches(jsonNode.get("key"), pattern));
    }

    @Test
    void testMatchesExactBooleanTrue() throws Exception {
        JsonNode jsonNode = objectMapper.createObjectNode().put("key", true);
        Patterns pattern = new ValuePatterns(MatchType.EXACT, "true");
        assertTrue(invokeMatches(jsonNode.get("key"), pattern));
    }

    @Test
    void testDoesNotMatchExactBooleanFalse() throws Exception {
        JsonNode jsonNode = objectMapper.createObjectNode().put("key", false);
        Patterns pattern = new ValuePatterns(MatchType.EXACT, "true");
        assertFalse(invokeMatches(jsonNode.get("key"), pattern));
    }
}
