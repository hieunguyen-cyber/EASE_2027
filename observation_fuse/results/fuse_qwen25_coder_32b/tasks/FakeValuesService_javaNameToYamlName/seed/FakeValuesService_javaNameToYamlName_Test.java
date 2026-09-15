package net.datafaker.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class FakeValuesService_javaNameToYamlName_Test {

    private FakeValuesService fakeValuesService;

    @BeforeEach
    void setupBeforeEach() {
        // Initialize the FakeValuesService before each test
        fakeValuesService = new FakeValuesService();
    }

    private String invokeJavaNameToYamlName(String expression) {
        try {
            // Use reflection to access the private method
            Method method = FakeValuesService.class.getDeclaredMethod("javaNameToYamlName", String.class);
            method.setAccessible(true);
            return (String) method.invoke(fakeValuesService, expression);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            fail("Failed to invoke method: " + e.getMessage());
            // This is here but unreachable, just for completion
            return null;
        }
    }

    @Test
    void testJavaNameToYamlName_UpperCamelCase() {
        String input = "UpperCamelCase";
        String expected = "upper_camel_case";
        String actual = invokeJavaNameToYamlName(input);
        assertEquals(expected, actual);
    }

    @Test
    void testJavaNameToYamlName_LowerCamelCase() {
        String input = "lowerCamelCase";
        String expected = "lower_camel_case";
        String actual = invokeJavaNameToYamlName(input);
        assertEquals(expected, actual);
    }

    @Test
    void testJavaNameToYamlName_NonCamelCase() {
        String input = "non_camel_case";
        String expected = "non_camel_case";
        String actual = invokeJavaNameToYamlName(input);
        assertEquals(expected, actual);
    }

    @Test
    void testJavaNameToYamlName_EmptyString() {
        String input = "";
        String expected = "";
        String actual = invokeJavaNameToYamlName(input);
        assertEquals(expected, actual);
    }

    @Test
    void testJavaNameToYamlName_SingleUpperCaseLetter() {
        String input = "A";
        String expected = "a";
        String actual = invokeJavaNameToYamlName(input);
        assertEquals(expected, actual);
    }

    @Test
    void testJavaNameToYamlName_NumericStart() {
        String input = "1Key";
        String expected = "1_key";
        String actual = invokeJavaNameToYamlName(input);
        assertEquals(expected, actual);
    }

    @Test
    void testJavaNameToYamlName_AlreadyFormatted() {
        String input = "already_formatted";
        String expected = "already_formatted";
        String actual = invokeJavaNameToYamlName(input);
        assertEquals(expected, actual);
    }

    @Test
    void testJavaNameToYamlName_RepeatedRequests() {
        String input = "RepeatKey";
        String expected = "repeat_key";
        String firstCall = invokeJavaNameToYamlName(input);
        assertEquals(expected, firstCall);
        String secondCall = invokeJavaNameToYamlName(input);
        assertEquals(expected, secondCall);
        // Change assertNotSame to assertEquals for content check
        assertEquals(firstCall, secondCall);
    }

    @ParameterizedTest
    @ValueSource(strings = { "UpperCase", "lowercase", "MixedCase" })
    void testJavaNameToYamlName_ParameterizedValidCase(String input) {
        String expected = input.replaceAll("(?<!^)(?=[A-Z])", "_").toLowerCase();
        assertEquals(expected, invokeJavaNameToYamlName(input));
    }

    // New test methods for exception scenarios
    @Test
    void testJavaNameToYamlName_NullInput() {
        String input = null;
        String actual = invokeJavaNameToYamlName(input);
        assertNull(actual, "The method should handle null inputs gracefully.");
    }

    @Test
    void testJavaNameToYamlName_SpecialCharacters() {
        String input = "Special@Character!";
        String expected = "special@character!";
        String actual = invokeJavaNameToYamlName(input);
        assertEquals(expected, actual, "Method must handle special characters appropriately.");
    }
}
