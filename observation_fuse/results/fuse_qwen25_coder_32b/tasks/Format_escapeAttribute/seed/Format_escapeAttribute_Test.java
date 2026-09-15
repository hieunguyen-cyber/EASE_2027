package org.jdom2.output;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class Format_escapeAttribute_Test {

    private Format format;

    private EscapeStrategy escapeStrategy;

    @BeforeEach
    void setupBeforeEach() {
        // Initialize the Format object and EscapeStrategy before each test
        format = new Format();
        escapeStrategy = mock(EscapeStrategy.class);
        // Optionally set any expectation on the mocked escapeStrategy if needed
    }

    @Test
    void testEscapeAttribute_withValidString() {
        // Test with a normal string with no special characters.
        // The output should be the same as the input.
        String value = "No special chars";
        when(escapeStrategy.shouldEscape(anyChar())).thenReturn(false);
        String result = Format.escapeAttribute(escapeStrategy, value);
        assertEquals(value, result);
    }

    @Test
    void testEscapeAttribute_withEscapableCharacters() {
        // Test with a string containing < and >.
        // The special characters should be escaped.
        String value = "<tag>";
        when(escapeStrategy.shouldEscape(anyChar())).thenAnswer(invocation -> {
            char ch = invocation.getArgument(0);
            return ch == '<' || ch == '>';
        });
        String result = Format.escapeAttribute(escapeStrategy, value);
        assertEquals("&lt;tag&gt;", result);
    }

    @Test
    void testEscapeAttribute_withAmpersand() {
        // Test with a string containing &.
        // The & character should be escaped as &amp;.
        String value = "Hello & World";
        // All chars should be escaped
        when(escapeStrategy.shouldEscape(anyChar())).thenReturn(true);
        String result = Format.escapeAttribute(escapeStrategy, value);
        assertEquals("Hello &amp; World", result);
    }

    @Test
    void testEscapeAttribute_withQuotes() {
        // Test with a string containing a double quote.
        // The " character should be escaped.
        String value = "Quote: \"text\"";
        // All chars should be escaped
        when(escapeStrategy.shouldEscape(anyChar())).thenReturn(true);
        String result = Format.escapeAttribute(escapeStrategy, value);
        assertEquals("Quote: &quot;text&quot;", result);
    }

    @Test
    void testEscapeAttribute_withNewLine() {
        // Test with a string containing a new line character.
        // It should be escaped to &#xA.
        String value = "Line break: \n New line";
        when(escapeStrategy.shouldEscape(anyChar())).thenReturn(true);
        String result = Format.escapeAttribute(escapeStrategy, value);
        assertEquals("Line break: &#xA; New line", result);
    }

    @Test
    void testEscapeAttribute_withTabCharacter() {
        // Test with a string containing a tab character.
        // The tab should be escaped to &#x9.
        String value = "\tTab character";
        when(escapeStrategy.shouldEscape(anyChar())).thenReturn(true);
        String result = Format.escapeAttribute(escapeStrategy, value);
        assertEquals("&#x9;Tab character", result);
    }

    @Test
    void testEscapeAttribute_withSurrogatePair() {
        // Test with a surrogate pair character.
        // The character should be escaped correctly.
        // Grinning Face emoji
        String value = "Surrogate pair test: \uD83D\uDE00";
        when(escapeStrategy.shouldEscape(anyChar())).thenReturn(true);
        String result = Format.escapeAttribute(escapeStrategy, value);
        assertEquals("Surrogate pair test: &#x1f600;", result);
    }
}
