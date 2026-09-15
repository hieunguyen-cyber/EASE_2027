package org.jdom2;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class Verifier_isXMLPublicIDCharacter_Test {

    private Verifier verifier;

    // Test method for isXMLPublicIDCharacter
    @Test
    void testIsXMLPublicIDCharacter_WithValidCharacters() {
        assertTrue(Verifier.isXMLPublicIDCharacter('a'), "Expected 'a' to be a valid PublicID character.");
        assertTrue(Verifier.isXMLPublicIDCharacter('Z'), "Expected 'Z' to be a valid PublicID character.");
        assertTrue(Verifier.isXMLPublicIDCharacter('0'), "Expected '0' to be a valid PublicID character.");
        assertTrue(Verifier.isXMLPublicIDCharacter(' '), "Expected space to be a valid PublicID character.");
        assertTrue(Verifier.isXMLPublicIDCharacter('#'), "Expected '#' to be a valid PublicID character.");
        assertTrue(Verifier.isXMLPublicIDCharacter('!'), "Expected '!' to be a valid PublicID character.");
        assertTrue(Verifier.isXMLPublicIDCharacter('_'), "Expected '_' to be a valid PublicID character.");
        assertTrue(Verifier.isXMLPublicIDCharacter('%'), "Expected '%' to be a valid PublicID character.");
        assertTrue(Verifier.isXMLPublicIDCharacter('\n'), "Expected newline to be a valid PublicID character.");
        assertTrue(Verifier.isXMLPublicIDCharacter('\r'), "Expected carriage return to be a valid PublicID character.");
        assertTrue(Verifier.isXMLPublicIDCharacter('\t'), "Expected tab to be a valid PublicID character.");
    }

    @Test
    void testIsXMLPublicIDCharacter_WithInvalidCharacters() {
        assertFalse(Verifier.isXMLPublicIDCharacter('\0'), "Expected null character to be invalid.");
        assertFalse(Verifier.isXMLPublicIDCharacter('<'), "Expected '<' to be invalid.");
        assertFalse(Verifier.isXMLPublicIDCharacter('>'), "Expected '>' to be invalid.");
        assertFalse(Verifier.isXMLPublicIDCharacter('&'), "Expected '&' to be invalid.");
        assertFalse(Verifier.isXMLPublicIDCharacter('['), "Expected '[' to be invalid.");
        assertFalse(Verifier.isXMLPublicIDCharacter(']'), "Expected ']' to be invalid.");
        assertFalse(Verifier.isXMLPublicIDCharacter('{'), "Expected '{' to be invalid.");
        assertFalse(Verifier.isXMLPublicIDCharacter('}'), "Expected '}' to be invalid.");
        assertFalse(Verifier.isXMLPublicIDCharacter('\"'), "Expected '\"' to be invalid.");
    }

    // Parameterized test for edge cases
    @ParameterizedTest
    @ValueSource(chars = { '-', '(', ')', ',', '.', '/', ':', ';', '*', '=', '?', '@' })
    void testIsXMLPublicIDCharacter_WithReservedCharacters(char c) {
        assertTrue(Verifier.isXMLPublicIDCharacter(c), "Expected '" + c + "' to be a valid PublicID character.");
    }

    @ParameterizedTest
    @ValueSource(chars = { '^', '`', '~', '©', '®' })
    void testIsXMLPublicIDCharacter_WithNonValidCharacters(char c) {
        assertFalse(Verifier.isXMLPublicIDCharacter(c), "Expected '" + c + "' to be invalid.");
    }

    // Additional test to leverage reflection and check exception scenarios
    @Test
    void testIsXMLPublicIDCharacter_Reflection_InvalidCharacter() throws Exception {
        Method method = Verifier.class.getDeclaredMethod("isXMLPublicIDCharacter", char.class);
        method.setAccessible(true);
        assertFalse((Boolean) method.invoke(null, '^'), "Expected '^' to be invalid.");
    }
@Test
void testBoundaryConditionLowercaseLetters() {
    assertTrue(Verifier.isXMLPublicIDCharacter('a'), "Expected 'a' to be a valid PublicID character.");
    assertTrue(Verifier.isXMLPublicIDCharacter('z'), "Expected 'z' to be a valid PublicID character.");
    assertFalse(Verifier.isXMLPublicIDCharacter('`'), "Expected '`' to be invalid.");
    assertFalse(Verifier.isXMLPublicIDCharacter('{'), "Expected '{' to be invalid.");
}
@Test
void testBoundaryConditionSpecialCharacters() {
    assertTrue(Verifier.isXMLPublicIDCharacter('\''), "Expected ''' to be a valid PublicID character.");
    assertTrue(Verifier.isXMLPublicIDCharacter(';'), "Expected ';' to be a valid PublicID character.");
    assertFalse(Verifier.isXMLPublicIDCharacter('\"'), "Expected '\"' to be invalid.");
    assertFalse(Verifier.isXMLPublicIDCharacter('<'), "Expected '<' to be invalid.");
}
@Test
void testSpecificCharacterDollar() {
    assertTrue(Verifier.isXMLPublicIDCharacter('$'), "Expected '$' to be a valid PublicID character.");
}
@Test
void testSpecificCharacterDollarFalse() {
    assertFalse(Verifier.isXMLPublicIDCharacter('€'), "Expected '€' to be invalid.");
}
}