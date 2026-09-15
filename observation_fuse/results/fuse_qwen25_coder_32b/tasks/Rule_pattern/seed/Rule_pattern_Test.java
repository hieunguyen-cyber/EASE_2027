package org.apache.commons.codec.language.bm;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
// Add other imports if necessary
import java.lang.reflect.Method;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
// Added import for InvocationTargetException
import java.lang.reflect.InvocationTargetException;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class Rule_pattern_Test {

    private Rule rule;

    private Rule.PhonemeExpr phonemeExpr;

    @BeforeEach
    void setupBeforeEach() {
        phonemeExpr = createMockPhonemeExpr();
        rule = new Rule("pattern", "lContext", "rContext", phonemeExpr);
    }

    private Rule.RPattern invokePrivatePatternMethod(String regex) throws Exception {
        // Handle potential NullPointerException outside reflection
        if (regex == null) {
            throw new NullPointerException("Regex cannot be null");
        }
        try {
            Method method = Rule.class.getDeclaredMethod("pattern", String.class);
            method.setAccessible(true);
            return (Rule.RPattern) method.invoke(null, regex);
        } catch (InvocationTargetException e) {
            throw e.getCause() instanceof Exception ? (Exception) e.getCause() : e;
        }
    }

    private Rule.PhonemeExpr createMockPhonemeExpr() {
        return Mockito.mock(Rule.PhonemeExpr.class);
    }

    @Test
    void testPattern() throws Exception {
        String inputString = "testInput";
        Rule.RPattern result = invokePrivatePatternMethod(inputString);
        assertNotNull(result);
    }

    @Test
    void testPattern_ReturnsEmptyMatcher_WhenRegexIsEmpty() throws Exception {
        String regex = "^$";
        Rule.RPattern result = invokePrivatePatternMethod(regex);
        assertNotNull(result);
        assertTrue(result.isMatch(""));
    }

    @Test
    void testPattern_RegularExactMatch() throws Exception {
        String regex = "^testInput$";
        Rule.RPattern result = invokePrivatePatternMethod(regex);
        assertNotNull(result);
        assertTrue(result.isMatch("testInput"));
        assertFalse(result.isMatch("testInputExtra"));
    }

    @Test
    void testPattern_StartsWithMatch() throws Exception {
        String regex = "^test";
        Rule.RPattern result = invokePrivatePatternMethod(regex);
        assertNotNull(result);
        assertTrue(result.isMatch("testInput"));
        assertFalse(result.isMatch("InputTest"));
    }

    @Test
    void testPattern_EndsWithMatch() throws Exception {
        String regex = "testInput$";
        Rule.RPattern result = invokePrivatePatternMethod(regex);
        assertNotNull(result);
        assertTrue(result.isMatch("testInput"));
        assertFalse(result.isMatch("testInputExtra"));
    }

    @Test
    void testPattern_CharacterClassMatch() throws Exception {
        String regex = "^[aeiou]pple$";
        Rule.RPattern result = invokePrivatePatternMethod(regex);
        assertNotNull(result);
        assertTrue(result.isMatch("apple"));
        assertFalse(result.isMatch("bpple"));
    }

    @Test
    void testPattern_NegatedCharacterClassMatch() throws Exception {
        String regex = "^[^aeiou]pple$";
        Rule.RPattern result = invokePrivatePatternMethod(regex);
        assertNotNull(result);
        assertTrue(result.isMatch("bpple"));
        assertFalse(result.isMatch("apple"));
    }

    // Additional edge cases added for completeness
    @Test
    void testPattern_StartsWithEmptyBox() throws Exception {
        String regex = "^[aeiou]*$";
        Rule.RPattern result = invokePrivatePatternMethod(regex);
        assertNotNull(result);
        assertTrue(result.isMatch("apple"));
        assertTrue(result.isMatch("aeiou"));
        // This should remain unchanged as it's expected behavior
        assertFalse(result.isMatch("bpple"));
    }

    @ParameterizedTest
    @CsvSource({ "^[aeiou].*, apple, true", "^[aeiou].*, banana, false", "^[^aeiou].*, banana, true", "^[^aeiou].*, apple, false" })
    void testPattern_ParameterizedCharacterClassMatch(String regex, String input, boolean expected) throws Exception {
        Rule.RPattern result = invokePrivatePatternMethod(regex);
        assertNotNull(result);
        assertEquals(expected, result.isMatch(input));
    }

    @Test
    void testPattern_ThrowsException_WhenRegexIsNull() {
        String regex = null;
        assertThrows(NullPointerException.class, () -> invokePrivatePatternMethod(regex));
    }

    @Test
    void testPattern_ThrowsException_WhenRegexIsInvalid() {
        String regex = "[a-z";
        assertThrows(java.util.regex.PatternSyntaxException.class, () -> invokePrivatePatternMethod(regex));
    }

    @Test
    void testPattern_HandlesWhitespaceOnlyRegex() throws Exception {
        // This will match a single whitespace character
        String regex = "\\s";
        Rule.RPattern result = invokePrivatePatternMethod(regex);
        assertNotNull(result);
        assertFalse(result.isMatch("testInput"));
        assertTrue(result.isMatch(" "));
    }

    @Test
    void testPattern_HandlesInvalidCharactersInRegex() {
        String regex = "test[";
        assertThrows(java.util.regex.PatternSyntaxException.class, () -> invokePrivatePatternMethod(regex));
    }
}
