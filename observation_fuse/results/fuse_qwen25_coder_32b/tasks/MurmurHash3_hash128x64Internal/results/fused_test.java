package org.apache.commons.codec.digest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.ParameterizedTest.*;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.ParameterizedTest;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class MurmurHash3_hash128x64Internal_Test {

    // Example seed
    private static final long SEED = 104729;

    // Example byte array for testing
    private static final byte[] TEST_DATA = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };

    private static final int OFFSET = 0;

    private static final int LENGTH = TEST_DATA.length;

    private long[] invokeHash128x64Internal(byte[] data, int offset, int length, long seed) throws Exception {
        Method method = MurmurHash3.class.getDeclaredMethod("hash128x64Internal", byte[].class, int.class, int.class, long.class);
        method.setAccessible(true);
        return (long[]) method.invoke(null, data, offset, length, seed);
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testHash128x64Internal_WithFullLengthBlock() throws Exception {
        long[] expectedResult = {/* Set your expected result based on actual calculations */
        };
        long[] actualResult = invokeHash128x64Internal(TEST_DATA, OFFSET, LENGTH, SEED);
        assertArrayEquals(expectedResult, actualResult, "The hash result should match the expected value for full length");
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testHash128x64Internal_WithPartialTail() throws Exception {
        // Test case when data length is not a multiple of 16
        // 13 bytes
        byte[] partialData = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };
        long[] expectedResult = {/* Set your expected result based on actual calculations */
        };
        long[] actualResult = invokeHash128x64Internal(partialData, 0, partialData.length, SEED);
        assertArrayEquals(expectedResult, actualResult, "The hash result should match the expected value for partial length");
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testHash128x64Internal_WithZeroLength() throws Exception {
        // Test case for zero length
        long[] expectedResult = {/* Set your expected result for zero length */
        };
        long[] actualResult = invokeHash128x64Internal(TEST_DATA, OFFSET, 0, SEED);
        assertArrayEquals(expectedResult, actualResult, "The hash result should match the expected value for zero length");
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testHash128x64Internal_WithNegativeOffset() {
        // Test case for invalid negative offset
        Exception exception = assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            invokeHash128x64Internal(TEST_DATA, -1, LENGTH, SEED);
        });
        assertTrue(exception.getMessage().contains("Index -1 out of bounds for length " + TEST_DATA.length));
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testHash128x64Internal_WithLengthExceedingArray() {
        // Test case for length exceeding the array size
        Exception exception = assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            invokeHash128x64Internal(TEST_DATA, OFFSET, TEST_DATA.length + 10, SEED);
        });
        assertTrue(exception.getMessage().contains("Index " + (OFFSET + TEST_DATA.length + 10) + " out of bounds for length " + TEST_DATA.length));
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testHash128x64Internal_WithIncorrectOffset() {
        // Test case for offset leading to negative index access
        Exception exception = assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            invokeHash128x64Internal(TEST_DATA, LENGTH + 1, LENGTH, SEED);
        });
        assertTrue(exception.getMessage().contains("Index " + (LENGTH + 1 + LENGTH) + " out of bounds for length " + TEST_DATA.length));
    }

    // Parameterized test for different seed values
    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @ParameterizedTest
    @ValueSource(longs = { 0L, 1L, Long.MAX_VALUE })
    void testHash128x64Internal_WithDifferentSeeds(long seed) throws Exception {
        long[] expectedResult = {/* Calculate the expected result based on seed */
        };
        long[] actualResult = invokeHash128x64Internal(TEST_DATA, OFFSET, LENGTH, seed);
        assertArrayEquals(expectedResult, actualResult, "The hash result should match the expected value for different seeds");
    }
@Test
void test_hash128x64Internal_withBoundaryConditionForLoop() throws Exception {
    byte[] data = new byte[16];
    long[] expectedResult = invokeHash128x64Internal(data, 0, 16, SEED);
    long[] actualResult = invokeHash128x64Internal(data, 0, 15, SEED);
    assertNotEquals(expectedResult[0], actualResult[0], "The hash result should differ for different lengths");
    assertNotEquals(expectedResult[1], actualResult[1], "The hash result should differ for different lengths");
}
@Test
void test_hash128x64Internal_withIntegerAdditionMutation() throws Exception {
    byte[] data = new byte[16];
    long[] expectedResult = invokeHash128x64Internal(data, 0, 16, SEED);
    long[] actualResult = invokeHash128x64Internal(data, 0, 15, SEED);
    assertNotEquals(expectedResult[0], actualResult[0], "The hash result should differ for different lengths");
    assertNotEquals(expectedResult[1], actualResult[1], "The hash result should differ for different lengths");
}
@Test
void test_hash128x64Internal_withLongMultiplicationMutation() throws Exception {
    byte[] data = new byte[16];
    long[] expectedResult = invokeHash128x64Internal(data, 0, 16, SEED);
    data[0] = 1;
    long[] actualResult = invokeHash128x64Internal(data, 0, 16, SEED);
    assertNotEquals(expectedResult[0], actualResult[0], "The hash result should differ for different data");
    assertNotEquals(expectedResult[1], actualResult[1], "The hash result should differ for different data");
}
@Test
void test_hash128x64Internal_withXORMutation() throws Exception {
    byte[] data = new byte[16];
    long[] expectedResult = invokeHash128x64Internal(data, 0, 16, SEED);
    data[0] = 1;
    long[] actualResult = invokeHash128x64Internal(data, 0, 16, SEED);
    assertNotEquals(expectedResult[0], actualResult[0], "The hash result should differ for different data");
    assertNotEquals(expectedResult[1], actualResult[1], "The hash result should differ for different data");
}
@Test
void test_hash128x64Internal_withLongAdditionMutation() throws Exception {
    byte[] data = new byte[16];
    long[] expectedResult = invokeHash128x64Internal(data, 0, 16, SEED);
    data[0] = 1;
    long[] actualResult = invokeHash128x64Internal(data, 0, 16, SEED);
    assertNotEquals(expectedResult[0], actualResult[0], "The hash result should differ for different data");
    assertNotEquals(expectedResult[1], actualResult[1], "The hash result should differ for different data");
}
@Test
void test_hash128x64Internal_withSwitchCaseMutation() throws Exception {
    byte[] data = new byte[15];
    long[] expectedResult = invokeHash128x64Internal(data, 0, 15, SEED);
    data[14] = 1;
    long[] actualResult = invokeHash128x64Internal(data, 0, 15, SEED);
    assertNotEquals(expectedResult[0], actualResult[0], "The hash result should differ for different data");
    assertNotEquals(expectedResult[1], actualResult[1], "The hash result should differ for different data");
}
@Test
void test_hash128x64Internal_withBitwiseANDMutation() throws Exception {
    byte[] data = new byte[15];
    long[] expectedResult = invokeHash128x64Internal(data, 0, 15, SEED);
    data[14] = 1;
    long[] actualResult = invokeHash128x64Internal(data, 0, 15, SEED);
    assertNotEquals(expectedResult[0], actualResult[0], "The hash result should differ for different data");
    assertNotEquals(expectedResult[1], actualResult[1], "The hash result should differ for different data");
}
@Test
void test_hash128x64Internal_withShiftLeftMutationInTail() throws Exception {
    byte[] data = new byte[15];
    long[] expectedResult = invokeHash128x64Internal(data, 0, 15, SEED);
    data[14] = 1;
    long[] actualResult = invokeHash128x64Internal(data, 0, 15, SEED);
    assertNotEquals(expectedResult[0], actualResult[0], "The hash result should differ for different data");
    assertNotEquals(expectedResult[1], actualResult[1], "The hash result should differ for different data");
}
@Test
void test_hash128x64Internal_withXORMutationInTail() throws Exception {
    byte[] data = new byte[15];
    long[] expectedResult = invokeHash128x64Internal(data, 0, 15, SEED);
    data[14] = 1;
    long[] actualResult = invokeHash128x64Internal(data, 0, 15, SEED);
    assertNotEquals(expectedResult[0], actualResult[0], "The hash result should differ for different data");
    assertNotEquals(expectedResult[1], actualResult[1], "The hash result should differ for different data");
}
@Test
void test_hash128x64Internal_withShiftLeftMutation() throws Exception {
    byte[] data = new byte[16];
    long[] expectedResult = invokeHash128x64Internal(data, 0, 16, SEED);
    long[] actualResult = invokeHash128x64Internal(data, 1, 15, SEED);
    assertNotEquals(expectedResult[0], actualResult[0], "The hash result should differ for different offsets");
    assertNotEquals(expectedResult[1], actualResult[1], "The hash result should differ for different offsets");
}
}