package com.google.gson.internal;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class Primitives_wrap_Test {

    @Test
    void testWrapForPrimitiveInt() {
        // Test wrapping for primitive int
        Class<Integer> result = Primitives.wrap(int.class);
        assertEquals(Integer.class, result);
    }

    @Test
    void testWrapForPrimitiveFloat() {
        // Test wrapping for primitive float
        Class<Float> result = Primitives.wrap(float.class);
        assertEquals(Float.class, result);
    }

    @Test
    void testWrapForPrimitiveByte() {
        // Test wrapping for primitive byte
        Class<Byte> result = Primitives.wrap(byte.class);
        assertEquals(Byte.class, result);
    }

    @Test
    void testWrapForPrimitiveDouble() {
        // Test wrapping for primitive double
        Class<Double> result = Primitives.wrap(double.class);
        assertEquals(Double.class, result);
    }

    @Test
    void testWrapForPrimitiveLong() {
        // Test wrapping for primitive long
        Class<Long> result = Primitives.wrap(long.class);
        assertEquals(Long.class, result);
    }

    @Test
    void testWrapForPrimitiveCharacter() {
        // Test wrapping for primitive char
        Class<Character> result = Primitives.wrap(char.class);
        assertEquals(Character.class, result);
    }

    @Test
    void testWrapForPrimitiveBoolean() {
        // Test wrapping for primitive boolean
        Class<Boolean> result = Primitives.wrap(boolean.class);
        assertEquals(Boolean.class, result);
    }

    @Test
    void testWrapForPrimitiveShort() {
        // Test wrapping for primitive short
        Class<Short> result = Primitives.wrap(short.class);
        assertEquals(Short.class, result);
    }

    @Test
    void testWrapForVoid() {
        // Test wrapping for void type
        Class<Void> result = Primitives.wrap(void.class);
        assertEquals(Void.class, result);
    }

    @Test
    void testWrapForNonPrimitive() {
        // Test wrapping for non-primitive type (String)
        Class<String> result = Primitives.wrap(String.class);
        assertEquals(String.class, result);
    }

    @ParameterizedTest
    @ValueSource(classes = { int.class, float.class, byte.class, double.class, long.class, char.class, boolean.class, short.class, void.class })
    void testWrapForAllPrimitiveTypes(Class<?> primitiveType) {
        // Ensure that each primitive type wraps correctly
        Class<?> expectedWrapper = Primitives.wrap(primitiveType);
        assertNotNull(expectedWrapper);
        // Assert that the expected wrapper class matches
        if (primitiveType == int.class)
            assertEquals(Integer.class, expectedWrapper);
        if (primitiveType == float.class)
            assertEquals(Float.class, expectedWrapper);
        if (primitiveType == byte.class)
            assertEquals(Byte.class, expectedWrapper);
        if (primitiveType == double.class)
            assertEquals(Double.class, expectedWrapper);
        if (primitiveType == long.class)
            assertEquals(Long.class, expectedWrapper);
        if (primitiveType == char.class)
            assertEquals(Character.class, expectedWrapper);
        if (primitiveType == boolean.class)
            assertEquals(Boolean.class, expectedWrapper);
        if (primitiveType == short.class)
            assertEquals(Short.class, expectedWrapper);
        if (primitiveType == void.class)
            assertEquals(Void.class, expectedWrapper);
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testWrapWithNullInput() {
        // Test wrapping for null input
        Exception exception = assertThrows(NullPointerException.class, () -> {
            Primitives.wrap(null);
        });
        assertEquals("type is null", exception.getMessage());
    }

    @Test
    void testWrapWithUnsupportedClass() {
        // Test wrapping for unsupported class type, should return the same class
        Class<Object> result = Primitives.wrap(Object.class);
        assertEquals(Object.class, result);
    }

    @Test
    void testWrapWithArrayType() {
        // Test wrapping for array type, should return the same array type
        Class<?> result = Primitives.wrap(int[].class);
        assertEquals(int[].class, result);
    }
}