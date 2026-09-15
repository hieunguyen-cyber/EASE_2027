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

class Ruler_matches_4_3_Test_slice4 {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private boolean invokeMatches(JsonNode jsonNode, Patterns pattern) {
        try {
            Method method = Ruler.class.getDeclaredMethod("matches", JsonNode.class, Patterns.class);
            method.setAccessible(true);
            return (boolean) method.invoke(null, jsonNode, pattern);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testMatchesExactString() {
        JsonNode jsonNode = objectMapper.createObjectNode().put("key", "value");
        Patterns pattern = new ValuePatterns(MatchType.EXACT, "value");
        assertTrue(invokeMatches(jsonNode.get("key"), pattern));
    }

    @Test
    void testMatchesExactStringWithQuotes() {
        JsonNode jsonNode = objectMapper.createObjectNode().put("key", "value");
        Patterns pattern = new ValuePatterns(MatchType.EXACT, "\"value\"");
        assertTrue(invokeMatches(jsonNode.get("key"), pattern));
    }

    @Test
    void testDoesNotMatchExactString() {
        JsonNode jsonNode = objectMapper.createObjectNode().put("key", "value");
        Patterns pattern = new ValuePatterns(MatchType.EXACT, "different");
        assertFalse(invokeMatches(jsonNode.get("key"), pattern));
    }

    @Test
    void testMatchesExactNumber() {
        JsonNode jsonNode = objectMapper.createObjectNode().put("key", 123);
        Patterns pattern = new ValuePatterns(MatchType.EXACT, ComparableNumber.generate(123));
        assertTrue(invokeMatches(jsonNode.get("key"), pattern));
    }

    @Test
    void testDoesNotMatchExactNumber() {
        JsonNode jsonNode = objectMapper.createObjectNode().put("key", 123);
        Patterns pattern = new ValuePatterns(MatchType.EXACT, ComparableNumber.generate(456));
        assertFalse(invokeMatches(jsonNode.get("key"), pattern));
    }

    @Test
    void testMatchesExactNull() {
        JsonNode jsonNode = objectMapper.createObjectNode().putNull("key");
        Patterns pattern = new ValuePatterns(MatchType.EXACT, "null");
        assertFalse(invokeMatches(jsonNode.get("key"), pattern));
    }

    @Test
    void testMatchesExactBooleanTrue() {
        JsonNode jsonNode = objectMapper.createObjectNode().put("key", true);
        Patterns pattern = new ValuePatterns(MatchType.EXACT, "true");
        assertTrue(invokeMatches(jsonNode.get("key"), pattern));
    }

    @Test
    void testDoesNotMatchExactBooleanFalse() {
        JsonNode jsonNode = objectMapper.createObjectNode().put("key", false);
        Patterns pattern = new ValuePatterns(MatchType.EXACT, "true");
        assertFalse(invokeMatches(jsonNode.get("key"), pattern));
    }
}
