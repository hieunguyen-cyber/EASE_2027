package org.apache.commons.cli;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.ParameterizedTest;

@Timeout(600)
class PatternOptionBuilder_getValueType_Test {

    private Method getValueTypeMethod;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        // Access the private method using reflection
        getValueTypeMethod = PatternOptionBuilder.class.getDeclaredMethod("getValueType", char.class);
        getValueTypeMethod.setAccessible(true);
    }

    @Test
    void testGetValueType() {
        assertEquals(PatternOptionBuilder.OBJECT_VALUE, PatternOptionBuilder.getValueType('@'));
        assertEquals(PatternOptionBuilder.STRING_VALUE, PatternOptionBuilder.getValueType(':'));
        assertEquals(PatternOptionBuilder.NUMBER_VALUE, PatternOptionBuilder.getValueType('%'));
        assertEquals(PatternOptionBuilder.CLASS_VALUE, PatternOptionBuilder.getValueType('+'));
        assertEquals(PatternOptionBuilder.DATE_VALUE, PatternOptionBuilder.getValueType('#'));
        assertEquals(PatternOptionBuilder.EXISTING_FILE_VALUE, PatternOptionBuilder.getValueType('<'));
        assertEquals(PatternOptionBuilder.FILE_VALUE, PatternOptionBuilder.getValueType('>'));
        assertEquals(PatternOptionBuilder.FILES_VALUE, PatternOptionBuilder.getValueType('*'));
        assertEquals(PatternOptionBuilder.URL_VALUE, PatternOptionBuilder.getValueType('/'));
        assertNull(PatternOptionBuilder.getValueType('x'), "Expected return to be null for unknown character");
    }

    // Test cases for the getValueType method to validate various character inputs
    @Test
    void testGetValueTypeForAllDefinedCharacters() {
        // Test for '@' returning OBJECT_VALUE
        assertEquals(PatternOptionBuilder.OBJECT_VALUE, PatternOptionBuilder.getValueType('@'), "Expected OBJECT_VALUE for character '@'");
        // Test for ':' returning STRING_VALUE
        assertEquals(PatternOptionBuilder.STRING_VALUE, PatternOptionBuilder.getValueType(':'), "Expected STRING_VALUE for character ':'");
        // Test for '%' returning NUMBER_VALUE
        assertEquals(PatternOptionBuilder.NUMBER_VALUE, PatternOptionBuilder.getValueType('%'), "Expected NUMBER_VALUE for character '%'");
        // Test for '+' returning CLASS_VALUE
        assertEquals(PatternOptionBuilder.CLASS_VALUE, PatternOptionBuilder.getValueType('+'), "Expected CLASS_VALUE for character '+'");
        // Test for '#' returning DATE_VALUE
        assertEquals(PatternOptionBuilder.DATE_VALUE, PatternOptionBuilder.getValueType('#'), "Expected DATE_VALUE for character '#'");
        // Test for '<' returning EXISTING_FILE_VALUE
        assertEquals(PatternOptionBuilder.EXISTING_FILE_VALUE, PatternOptionBuilder.getValueType('<'), "Expected EXISTING_FILE_VALUE for character '<'");
        // Test for '>' returning FILE_VALUE
        assertEquals(PatternOptionBuilder.FILE_VALUE, PatternOptionBuilder.getValueType('>'), "Expected FILE_VALUE for character '>'");
        // Test for '*' returning FILES_VALUE
        assertEquals(PatternOptionBuilder.FILES_VALUE, PatternOptionBuilder.getValueType('*'), "Expected FILES_VALUE for character '*'");
        // Test for '/' returning URL_VALUE
        assertEquals(PatternOptionBuilder.URL_VALUE, PatternOptionBuilder.getValueType('/'), "Expected URL_VALUE for character '/'");
    }

    // Test case for an undefined character
    @Test
    void testGetValueTypeForUndefinedCharacter() {
        // Test for an unknown character 'x' to ensure it returns null
        assertNull(PatternOptionBuilder.getValueType('x'), "Expected null for an undefined character 'x'");
    }

    @Test
    void testGetValueTypeDefinedCharacters() throws Exception {
        assertEquals(PatternOptionBuilder.OBJECT_VALUE, getValueTypeMethod.invoke(null, '@'));
        assertEquals(PatternOptionBuilder.STRING_VALUE, getValueTypeMethod.invoke(null, ':'));
        assertEquals(PatternOptionBuilder.NUMBER_VALUE, getValueTypeMethod.invoke(null, '%'));
        assertEquals(PatternOptionBuilder.CLASS_VALUE, getValueTypeMethod.invoke(null, '+'));
        assertEquals(PatternOptionBuilder.DATE_VALUE, getValueTypeMethod.invoke(null, '#'));
        assertEquals(PatternOptionBuilder.EXISTING_FILE_VALUE, getValueTypeMethod.invoke(null, '<'));
        assertEquals(PatternOptionBuilder.FILE_VALUE, getValueTypeMethod.invoke(null, '>'));
        assertEquals(PatternOptionBuilder.FILES_VALUE, getValueTypeMethod.invoke(null, '*'));
        assertEquals(PatternOptionBuilder.URL_VALUE, getValueTypeMethod.invoke(null, '/'));
    }

    @ParameterizedTest
    @ValueSource(chars = { '@', ':', '%', '+', '#', '<', '>', '*', '/' })
    void testGetValueTypeWithParameterizedInput(char input) throws Exception {
        Class<?> expected = switch(input) {
            case '@' ->
                PatternOptionBuilder.OBJECT_VALUE;
            case ':' ->
                PatternOptionBuilder.STRING_VALUE;
            case '%' ->
                PatternOptionBuilder.NUMBER_VALUE;
            case '+' ->
                PatternOptionBuilder.CLASS_VALUE;
            case '#' ->
                PatternOptionBuilder.DATE_VALUE;
            case '<' ->
                PatternOptionBuilder.EXISTING_FILE_VALUE;
            case '>' ->
                PatternOptionBuilder.FILE_VALUE;
            case '*' ->
                PatternOptionBuilder.FILES_VALUE;
            case '/' ->
                PatternOptionBuilder.URL_VALUE;
            default ->
                null;
        };
        assertEquals(expected, getValueTypeMethod.invoke(null, input), "Expected class type for character: " + input);
    }

    // New test methods to cover exception-throwing scenarios
    @Test
    void testGetValueTypeForInvalidCharacter() {
        // Test with an invalid character (e.g., '!')
        assertNull(PatternOptionBuilder.getValueType('!'), "Expected null for the invalid character '!'");
    }

    @Test
    void testGetValueTypeForNumericCharacter() {
        // Test with a numeric character (e.g., '1')
        assertNull(PatternOptionBuilder.getValueType('1'), "Expected null for a numeric character '1'");
    }

    // Additional test method for boundary check: testing with non-character inputs (if applicable, but here char type handles it)
    @Test
    void testGetValueTypeForSpecialCharacters() {
        // Test with special characters (e.g., '$', '&')
        assertNull(PatternOptionBuilder.getValueType('$'), "Expected null for the special character '$'");
        assertNull(PatternOptionBuilder.getValueType('&'), "Expected null for the special character '&'");
    }
}