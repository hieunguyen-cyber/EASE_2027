package net.datafaker.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Locale;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class FakeValuesService_bothify_Test {

    @Spy
    private FakeValuesService fakeValuesService;

    @Mock
    private RandomService randomService;

    private FakerContext context;

    @BeforeEach
    void setupBeforeEach() {
        // Initialize FakerContext
        context = new FakerContext(Locale.ENGLISH, randomService);
        // Spy on FakeValuesService
        fakeValuesService = Mockito.spy(new FakeValuesService());
        // Set up mocks for random service to return predictable values
        // Always return 0 for simplicity
        when(randomService.nextInt(anyInt())).thenReturn(0);
        // For '?' case, always return 1
        when(randomService.nextInt(1, 9)).thenReturn(1);
    }

    private String invokeBothify(String input, FakerContext context, boolean isUpper, boolean numerify, boolean letterify) {
        try {
            Method method = FakeValuesService.class.getDeclaredMethod("bothify", String.class, FakerContext.class, boolean.class, boolean.class, boolean.class);
            // Accessing the private method via reflection
            method.setAccessible(true);
            return (String) method.invoke(fakeValuesService, input, context, isUpper, numerify, letterify);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            fail("Reflection failed: " + e.getMessage());
            return null;
        }
    }

    @Test
    void bothifyWithUppercaseAndNumerify() {
        // Test case: Input string with upper case letters and numerify flag set to true
        String input = "??##??";
        String result = invokeBothify(input, context, true, true, false);
        assertNotNull(result);
        // Assert expected output
        assertEquals("AA00AA", result);
    }

    @Test
    void bothifyWithLowercaseAndLetterify() {
        // Test case: Input string with lower case letters and letterify flag set to true
        String input = "??##??";
        String result = invokeBothify(input, context, false, true, true);
        assertNotNull(result);
        // Assert expected output
        assertEquals("aa00aa", result);
    }

    @Test
    void bothifyWithDefaults() {
        // Test case: Input string with no special flags applied
        String input = "Hello ##";
        String result = invokeBothify(input, context, false, false, false);
        assertNotNull(result);
        // Assert default behavior, expecting '00'
        assertEquals("Hello 00", result);
    }

    @Test
    void bothifyOnlyNumerify() {
        // Test case: Ensure it handles only numerification
        String input = "##";
        String result = invokeBothify(input, context, false, true, false);
        // Expect '00'
        assertEquals("00", result);
    }

    @Test
    void bothifyOnlyLetterify() {
        // Test case: Input string with only letterify enabled
        String input = "??";
        String result = invokeBothify(input, context, false, false, true);
        assertNotNull(result);
        assertTrue(result.matches("[a-z]{2}"), "Result should consist of two lowercase letters.");
    }

    @Test
    void bothifyMixedCaseAndFlags() {
        // Test case: Mixed input with various flags set
        String input = "Hello ?#??";
        String result = invokeBothify(input, context, true, true, true);
        assertNotNull(result);
        // Check the case of the first letter
        assertTrue(result.startsWith("H"));
        // The final characters expect based on setup
        assertEquals("H00aa", result);
    }

    @Test
    void bothifyWithExactNumerifyFlag() {
        // More complex pattern
        String input = "??###??";
        String result = invokeBothify(input, context, false, true, false);
        assertTrue(result.matches("[a-z]{2}[0-9]{3}[a-z]{2}"), "Result should consist of two lowercase letters, three digits, and then two lowercase letters.");
    }

    @Test
    void bothifyWithMixedNumerifyAndLetterify() {
        // Mixed case with letters and digits
        String input = "??#??##";
        String result = invokeBothify(input, context, true, true, true);
        // Check for uppercase at first and letter-digit combination
        assertTrue(result.startsWith("A"), "First character should be upper-case.");
        assertTrue(result.matches("[A-Z]{2}[0-9]{2}"), "Result should match the pattern.");
    }

    // Test 1: Test for Null Pointer Exception when context is null
    @Test
    void bothifyShouldThrowNullPointerExceptionWhenContextIsNull() {
        assertThrows(NullPointerException.class, () -> {
            invokeBothify("??##??", null, true, true, false);
        });
    }

    // Test 2: Test for IllegalArgumentException when input string is null
    @Test
    void bothifyShouldThrowIllegalArgumentExceptionWhenInputIsNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            invokeBothify(null, context, true, true, false);
        });
    }

    // Test 3: Test for IllegalArgumentException when the string input is empty
    @Test
    void bothifyShouldThrowIllegalArgumentExceptionWhenInputIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            invokeBothify("", context, true, false, true);
        });
    }
}
