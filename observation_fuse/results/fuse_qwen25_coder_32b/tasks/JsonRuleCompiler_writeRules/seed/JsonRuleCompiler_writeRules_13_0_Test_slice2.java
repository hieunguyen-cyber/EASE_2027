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
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.core.JsonParseException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

class JsonRuleCompiler_writeRules_13_0_Test_slice2 {

    @Test
    void testWriteRulesWithValidInput() {
        List<Map<String, List<Patterns>>> rules = new ArrayList<>();
        String name = "testRule";
        String jsonInput = "[{\"key\": \"value\"}, 123, null, true]";
        try {
            JsonParser parser = new JsonFactory().createParser(jsonInput);
            Method method = JsonRuleCompiler.class.getDeclaredMethod("writeRules", List.class, String.class, JsonParser.class, boolean.class);
            method.setAccessible(true);
            method.invoke(null, rules, name, parser, true);
            assertEquals(1, rules.size());
            assertEquals(4, rules.get(0).get(name).size());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception should not have been thrown.");
        }
    }

    @Test
    void testWriteRulesWithEmptyArray() {
        List<Map<String, List<Patterns>>> rules = new ArrayList<>();
        String name = "emptyRule";
        String jsonInput = "[]";
        try {
            JsonParser parser = new JsonFactory().createParser(jsonInput);
            Method method = JsonRuleCompiler.class.getDeclaredMethod("writeRules", List.class, String.class, JsonParser.class, boolean.class);
            method.setAccessible(true);
            Exception exception = assertThrows(IOException.class, () -> {
                method.invoke(null, rules, name, parser, true);
            });
            assertEquals("Empty arrays are not allowed", exception.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testWriteRulesWithInvalidJson() {
        List<Map<String, List<Patterns>>> rules = new ArrayList<>();
        String name = "invalidRule";
        String jsonInput = "[{invalidJson}";
        try {
            JsonParser parser = new JsonFactory().createParser(jsonInput);
            Method method = JsonRuleCompiler.class.getDeclaredMethod("writeRules", List.class, String.class, JsonParser.class, boolean.class);
            method.setAccessible(true);
            Exception exception = assertThrows(IOException.class, () -> {
                method.invoke(null, rules, name, parser, true);
            });
            assertNotNull(exception);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
