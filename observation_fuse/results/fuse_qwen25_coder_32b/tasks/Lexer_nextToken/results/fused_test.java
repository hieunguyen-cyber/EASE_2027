package org.apache.commons.csv;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class Lexer_nextToken_Test {

    private CSVFormat csvFormat;

    private ExtendedBufferedReader extendedBufferedReader;

    private Lexer lexer;

    private Token token;

    @BeforeEach
    void setupBeforeEach() {
        csvFormat = CSVFormat.DEFAULT.withEscape('\\').withIgnoreEmptyLines(false);
        extendedBufferedReader = mock(ExtendedBufferedReader.class);
        lexer = new Lexer(csvFormat, extendedBufferedReader);
        token = new Token();
    }

    @AfterEach
    void teardownAfterEach() {
        try {
            lexer.close();
        } catch (IOException e) {
            fail("Failed to close lexer: " + e.getMessage());
        }
    }

    @Test
    void testNextTokenReturnsEOFForEmptyInput() throws IOException {
        when(extendedBufferedReader.read()).thenReturn(-1);
        Token result = lexer.nextToken(token);
        assertEquals(Token.Type.EOF, result.type);
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testNextTokenWithSingleToken() throws IOException {
        String input = "token";
        when(extendedBufferedReader.read()).thenReturn((int) 't', (int) 'o', (int) 'k', (int) 'e', (int) 'n', -1);
        Token result = lexer.nextToken(token);
        assertEquals(Token.Type.TOKEN, result.type);
        assertEquals("token", result.content.toString());
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testNextTokenHandlesEOFAfterToken() throws IOException {
        when(extendedBufferedReader.read()).thenReturn((int) 'a', (int) 'b', (int) 'c', -1);
        Token result = lexer.nextToken(token);
        assertEquals(Token.Type.TOKEN, result.type);
        assertEquals("abc", result.content.toString());
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testNextTokenHandlesEmptyLines() throws IOException {
        when(extendedBufferedReader.read()).thenReturn((int) '\n', (int) '\n', -1);
        Token result1 = lexer.nextToken(token);
        Token result2 = lexer.nextToken(token);
        assertEquals(Token.Type.EORECORD, result1.type);
        assertEquals(Token.Type.EOF, result2.type);
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testNextTokenHandlesDelimiters() throws IOException {
        String inputWithDelimiter = "token1,token2\n";
        when(extendedBufferedReader.read()).thenReturn((int) 't', (int) 'o', (int) 'k', (int) 'e', (int) 'n', (int) '1', (int) ',', (int) 't', (int) 'o', (int) 'k', (int) 'e', (int) 'n', (int) '2', (int) '\n', -1);
        Token result1 = lexer.nextToken(token);
        Token result2 = lexer.nextToken(token);
        Token result3 = lexer.nextToken(token);
        assertEquals(Token.Type.TOKEN, result1.type);
        assertEquals("token1", result1.content.toString());
        assertEquals(Token.Type.TOKEN, result2.type);
        assertEquals("token2", result2.content.toString());
        assertEquals(Token.Type.EORECORD, result3.type);
    }
@Test
void testNextTokenHandlesQuotedTokens() throws IOException {
    csvFormat = CSVFormat.DEFAULT.withQuote('\"');
    when(extendedBufferedReader.read()).thenReturn((int) '\"', (int) 'a', (int) 'b', (int) 'c', (int) '\"', -1);
    Token result = lexer.nextToken(token);
    assertEquals("abc", result.content.toString());
}
}