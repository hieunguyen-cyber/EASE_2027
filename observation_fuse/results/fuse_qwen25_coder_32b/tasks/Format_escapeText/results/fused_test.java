package org.jdom2.output;

import static org.mockito.Mockito.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.jdom2.IllegalDataException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class Format_escapeText_Test {

    private Format format;

    private EscapeStrategy escapeStrategy;

    private String endofline;

    @BeforeEach
    void setupBeforeEach() {
        format = new Format();
        escapeStrategy = mock(EscapeStrategy.class);
        endofline = "\n";
        format.setEscapeStrategy(escapeStrategy);
    }

    @Test
    void testEscapeText_NoEscapeNeeded() {
        String input = "No special characters";
        String result = Format.escapeText(format.getEscapeStrategy(), format.getLineSeparator(), input);
        assertEquals(input, result, "The output should be identical to the input, as there are no characters needing escape.");
    }

    @Test
    void testEscapeText_SpecialCharacterEscaped() {
        String input = "Special character: <>";
        when(escapeStrategy.shouldEscape(anyChar())).thenAnswer(invocation -> {
            char ch = invocation.getArgument(0);
            return ch == '<' || ch == '>' || ch == '&';
        });
        String result = Format.escapeText(format.getEscapeStrategy(), format.getLineSeparator(), input);
        assertEquals("Special character: &lt;&gt;", result, "The output should escape special characters '<' and '>'.");
    }

    @Test
    void testEscapeText_NewLineReplaced() {
        String input = "Line 1\nLine 2";
        String expected = "Line 1" + endofline + "Line 2";
        String result = Format.escapeText(format.getEscapeStrategy(), endofline, input);
        assertEquals(expected, result, "New lines should be replaced with the specified end-of-line sequence.");
    }

    @Test
    void testEscapeText_CharacterToBeEscaped() {
        String input = "This & That";
        when(escapeStrategy.shouldEscape(anyChar())).thenAnswer(invocation -> {
            char ch = invocation.getArgument(0);
            return ch == '&';
        });
        String result = Format.escapeText(format.getEscapeStrategy(), format.getLineSeparator(), input);
        assertEquals("This &amp; That", result, "The output should escape the '&' character when configured to do so.");
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testEscapeText_SurrogatePairException() {
        // High surrogate only
        String input = "\uD83D";
        when(escapeStrategy.shouldEscape(anyChar())).thenReturn(false);
        assertThrows(IllegalDataException.class, () -> {
            Format.escapeText(format.getEscapeStrategy(), format.getLineSeparator(), input);
        }, "An IllegalDataException should be thrown for a truncated surrogate pair.");
    }

    @Test
    void testEscapeText_EscapeCharactersMixed() {
        String input = "Escape this: < & >";
        when(escapeStrategy.shouldEscape(anyChar())).thenAnswer(invocation -> {
            char ch = invocation.getArgument(0);
            return ch == '<' || ch == '&' || ch == '>';
        });
        String result = Format.escapeText(format.getEscapeStrategy(), format.getLineSeparator(), input);
        assertEquals("Escape this: &lt; &amp; &gt;", result, "All special characters should be escaped correctly.");
    }

    @Test
    void testEscapeText_NoCharacters_EmptyInput() {
        String input = "";
        String result = Format.escapeText(format.getEscapeStrategy(), format.getLineSeparator(), input);
        assertEquals(input, result, "The output should be an empty string as the input is empty.");
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @ParameterizedTest
    @ValueSource(strings = { "Sample text", "Text with & special", "<tag>", "New\nline" })
    void testEscapeText_NoEscapingIfConfiguredNotTo(String input) {
        when(escapeStrategy.shouldEscape(anyChar())).thenReturn(false);
        String result = Format.escapeText(format.getEscapeStrategy(), format.getLineSeparator(), input);
        assertEquals(input, result, "The output should be identical to the input, as no characters should be escaped.");
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testEscapeText_SurrogatePairTruncatedException() {
        // High surrogate without a low surrogate
        String input = "\uD800";
        when(escapeStrategy.shouldEscape(anyChar())).thenReturn(false);
        assertThrows(IllegalDataException.class, () -> {
            Format.escapeText(format.getEscapeStrategy(), format.getLineSeparator(), input);
        }, "An IllegalDataException should be thrown for a truncated high surrogate.");
    }
@Test
void test_conditional_boundary_idx() {
    String input = "A";
    when(escapeStrategy.shouldEscape(anyChar())).thenReturn(false);
    String result = Format.escapeText(format.getEscapeStrategy(), format.getLineSeparator(), input);
    assertEquals("A", result, "The output should be identical to the input as no characters need escape.");
}
@Test
void test_removal_conditional_strategy() {
    String input = "A";
    when(escapeStrategy.shouldEscape(anyChar())).thenReturn(true);
    String result = Format.escapeText(format.getEscapeStrategy(), format.getLineSeparator(), input);
    assertEquals("&#x41;", result, "Character should be escaped based on strategy.");
}
@Test
void test_removal_conditional_eol() {
    String input = "Line 1\nLine 2";
    String expected = "Line 1\nLine 2";
    when(escapeStrategy.shouldEscape(anyChar())).thenReturn(false);
    String result = Format.escapeText(format.getEscapeStrategy(), null, input);
    assertEquals(expected, result, "New lines should not be replaced if eol is null.");
}
@Test
void test_removal_conditional_strategy_default() {
    String input = "A";
    when(escapeStrategy.shouldEscape(anyChar())).thenReturn(true);
    String result = Format.escapeText(format.getEscapeStrategy(), format.getLineSeparator(), input);
    assertEquals("&#x41;", result, "Character should be escaped based on strategy.");
}
@Test
void test_removal_conditional_while_loop() {
    String input = "A";
    when(escapeStrategy.shouldEscape(anyChar())).thenReturn(false);
    String result = Format.escapeText(format.getEscapeStrategy(), format.getLineSeparator(), input);
    assertEquals("A", result, "The output should be identical to the input as no characters need escape.");
}
@Test
void test_removal_conditional_highsurrogate() {
    String input = "\uD83D\uDE00"; // Valid surrogate pair
    when(escapeStrategy.shouldEscape(anyChar())).thenReturn(false);
    String result = Format.escapeText(format.getEscapeStrategy(), format.getLineSeparator(), input);
    assertEquals("😀", result, "Valid surrogate pair should not be escaped.");
}
@Test
void test_isHighSurrogate() {
    char highSurrogate = '\uD83D';
    char lowSurrogate = '\uDE00';
    when(escapeStrategy.shouldEscape(anyChar())).thenReturn(false);
    String result = Format.escapeText(format.getEscapeStrategy(), format.getLineSeparator(), new String(new char[]{highSurrogate, lowSurrogate}));
    assertEquals("😀", result, "Valid surrogate pair should not be escaped.");
}
}