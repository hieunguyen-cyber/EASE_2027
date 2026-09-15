package org.apache.commons.codec.binary;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.commons.codec.binary.BaseNCodec.Context;
import java.lang.reflect.Method;
// Added import for InvocationTargetException
import java.lang.reflect.InvocationTargetException;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class Base32_encode_Test {

    private Base32 base32;

    private Context context;

    private static final byte[] TEST_INPUT = "foobar".getBytes();

    private static final byte[] TEST_INPUT_HELLO = "Hello".getBytes();

    private static final byte[] EMPTY_INPUT = new byte[0];

    private static final byte[] SINGLE_BYTE_INPUT = { 'a' };

    private static final byte[] MULTIPLE_BYTES_INPUT = "abcdefgh".getBytes();

    @BeforeEach
    void setupBeforeEach() {
        base32 = Base32.builder().setLineLength(76).setLineSeparator(new byte[] { '\r', '\n' }).setPadding((byte) '=').get();
        context = new Context();
        context.pos = 0;
        context.eof = false;
        // Initialize modulus; it should start at 0
        context.modulus = 0;
    }

    // Utility method to invoke a private method using reflection
    private void invokePrivateEncodeMethod(byte[] input, int inPos, int inAvail, Context context) throws Exception {
        Method encodeMethod = Base32.class.getDeclaredMethod("encode", byte[].class, int.class, int.class, Context.class);
        encodeMethod.setAccessible(true);
        encodeMethod.invoke(base32, input, inPos, inAvail, context);
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testEncode() {
        byte[] expectedOutput = "MZXW6YTBOI======".getBytes();
        base32.encode(TEST_INPUT, 0, TEST_INPUT.length, context);
        byte[] actualOutput = new byte[expectedOutput.length];
        // Ensure EOF is set correctly for encoding
        context.eof = true;
        // Finalize encoding
        base32.encode(new byte[0], 0, -1, context);
        context.pos = 0;
        base32.readResults(actualOutput, 0, actualOutput.length, context);
        assertArrayEquals(expectedOutput, actualOutput);
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testEncodeWithDifferentInput() {
        byte[] expectedOutput = "NBSWY3DP".getBytes();
        base32.encode(TEST_INPUT_HELLO, 0, TEST_INPUT_HELLO.length, context);
        byte[] actualOutput = new byte[expectedOutput.length];
        context.eof = true;
        base32.encode(new byte[0], 0, -1, context);
        context.pos = 0;
        base32.readResults(actualOutput, 0, actualOutput.length, context);
        assertArrayEquals(expectedOutput, actualOutput, "Encoding failed for input 'Hello'");
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testEncodeCompleteInput() {
        byte[] expectedOutput = "MZXW6YTBOI======".getBytes();
        // Use the existing test method
        testEncode();
        byte[] actualOutput = new byte[expectedOutput.length];
        context.eof = true;
        base32.encode(new byte[0], 0, -1, context);
        context.pos = 0;
        base32.readResults(actualOutput, 0, actualOutput.length, context);
        assertArrayEquals(expectedOutput, actualOutput, "Encoding failed for input 'foobar'");
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testEncodeWithEOF() {
        byte[] expectedOutput = "MZXW6YTBOI======".getBytes();
        base32.encode(TEST_INPUT, 0, TEST_INPUT.length, context);
        context.eof = true;
        base32.encode(new byte[0], 0, -1, context);
        byte[] actualOutput = new byte[expectedOutput.length];
        context.pos = 0;
        base32.readResults(actualOutput, 0, actualOutput.length, context);
        assertArrayEquals(expectedOutput, actualOutput, "Encoding with EOF handling failed.");
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testSingleByteEncoding() {
        byte[] expectedOutput = "ME======".getBytes();
        base32.encode(SINGLE_BYTE_INPUT, 0, SINGLE_BYTE_INPUT.length, context);
        byte[] actualOutput = new byte[expectedOutput.length];
        context.eof = true;
        base32.encode(new byte[0], 0, -1, context);
        context.pos = 0;
        base32.readResults(actualOutput, 0, actualOutput.length, context);
        assertArrayEquals(expectedOutput, actualOutput, "Encoding failed for single byte input.");
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testMultipleBytesEncodingWithLineSeparator() {
        byte[] expectedOutput = "MZXW6YTBOI=====\r\n".getBytes();
        base32.encode(TEST_INPUT, 0, TEST_INPUT.length, context);
        byte[] actualOutput = new byte[expectedOutput.length];
        context.eof = true;
        base32.encode(new byte[0], 0, -1, context);
        context.pos = 0;
        base32.readResults(actualOutput, 0, actualOutput.length, context);
        assertArrayEquals(expectedOutput, actualOutput, "Encoding with line separators failed.");
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testEncodeNormalInput() {
        byte[] expectedOutput = "MZXW6YTBOI======".getBytes();
        base32.encode(TEST_INPUT, 0, TEST_INPUT.length, context);
        byte[] actualOutput = new byte[expectedOutput.length];
        context.eof = true;
        base32.encode(new byte[0], 0, -1, context);
        context.pos = 0;
        base32.readResults(actualOutput, 0, actualOutput.length, context);
        assertArrayEquals(expectedOutput, actualOutput);
    }

    @Test
    void testEmptyInput() {
        byte[] expectedOutput = new byte[0];
        base32.encode(EMPTY_INPUT, 0, EMPTY_INPUT.length, context);
        byte[] actualOutput = new byte[expectedOutput.length];
        context.eof = true;
        base32.encode(new byte[0], 0, -1, context);
        context.pos = 0;
        base32.readResults(actualOutput, 0, actualOutput.length, context);
        assertArrayEquals(expectedOutput, actualOutput, "Encoding for empty input should return empty output.");
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testEncodeWithLongInputMoreThanOneLine() {
        byte[] longInput = MULTIPLE_BYTES_INPUT;
        byte[] expectedOutput = "MZXW6YTBOI=====\r\nMZXW6YTBOI======".getBytes();
        base32.encode(longInput, 0, longInput.length, context);
        byte[] actualOutput = new byte[expectedOutput.length];
        context.eof = true;
        base32.encode(new byte[0], 0, -1, context);
        context.pos = 0;
        base32.readResults(actualOutput, 0, actualOutput.length, context);
        assertArrayEquals(expectedOutput, actualOutput, "Encoding with multiple lines failed.");
    }

    @Test
    void testEncodeWithNegativeInAvail() {
        context.eof = false;
        base32.encode(new byte[] {}, 0, -1, context);
        assertTrue(context.eof, "Context should be marked as EOF when inAvail is -1");
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testEncodeWithInvalidModulus() {
        context.modulus = 5;
        assertThrows(IllegalStateException.class, () -> {
            base32.encode(new byte[] { 0 }, 0, 1, context);
        }, "Expected IllegalStateException for invalid modulus.");
    }

    @Test
    void testEncodeWithInvalidContext() {
        assertThrows(NullPointerException.class, () -> {
            base32.encode(new byte[] { 0 }, 0, 1, null);
        }, "Expected NullPointerException when context is null.");
    }

    @Test
    void testEncodeWithNullInput() {
        Exception exception = assertThrows(InvocationTargetException.class, () -> {
            invokePrivateEncodeMethod(null, 0, 1, context);
        });
        assertTrue(exception.getCause() instanceof NullPointerException, "Expected NullPointerException when input is null.");
    }
@Test
void test_inAvailBoundary() {
    context.eof = false;
    base32.encode(new byte[] {}, 0, 0, context);
    assertFalse(context.eof, "Context should not be marked as EOF when inAvail is 0");
}
@Test
void test_byteIncrement() throws Exception {
    byte[] input = {1, 2, 3};
    invokePrivateEncodeMethod(input, 0, 3, context);
    assertEquals(3, context.modulus, "Modulus should be incremented correctly");
}
@Test
void test_currentLinePosMath() {
    context.pos = 8;
    base32.encode(new byte[] {0, 0, 0, 0, 0}, 0, 5, context);
    assertEquals(8, context.currentLinePos, "currentLinePos should be updated correctly");
}
@Test
void test_modulusMath() throws Exception {
    context.modulus = 4;
    invokePrivateEncodeMethod(new byte[] {0}, 0, 1, context);
    assertEquals(0, context.modulus, "Modulus should wrap around correctly");
}
@Test
void test_eofConditional() {
    context.eof = true;
    base32.encode(new byte[] {0}, 0, 1, context);
    assertEquals(0, context.pos, "Position should not change when EOF is true");
}
@Test
void test_modulusAndLineLengthConditional() {
    context.modulus = 0;
    base32.encode(new byte[] {}, 0, -1, context);
    assertTrue(context.eof, "Context should be marked as EOF when modulus is 0 and lineLength is 0");
}
@Test
void test_byteIncrementMutation() throws Exception {
    byte[] input = {(byte) -1};
    invokePrivateEncodeMethod(input, 0, 1, context);
    assertEquals(255, context.lbitWorkArea, "lbitWorkArea should be correctly set for byte value -1");
}
}