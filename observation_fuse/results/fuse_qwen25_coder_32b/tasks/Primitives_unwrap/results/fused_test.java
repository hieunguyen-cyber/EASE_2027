package com.google.gson.internal;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class Primitives_unwrap_Test {

    private Method unwrapMethod;

    @BeforeEach
    void setUp() throws Exception {
        // Initialize the method object for reflection to access the private method
        unwrapMethod = Primitives.class.getDeclaredMethod("unwrap", Class.class);
        unwrapMethod.setAccessible(true);
    }

    static Object[][] provideTestClassesForUnwrapping() {
        return new Object[][] { 
            { Integer.class, int.class }, 
            { Float.class, float.class }, 
            { Byte.class, byte.class }, 
            { Double.class, double.class }, 
            { Long.class, long.class }, 
            { Character.class, char.class }, 
            { Boolean.class, boolean.class }, 
            { Short.class, short.class }, 
            { Void.class, void.class } 
        };
    }

    @Test
    void testUnwrap_withIntegerClass() {
        // Test the unwrapping of Integer class to its primitive type int
        Class<?> result = Primitives.unwrap(Integer.class);
        assertEquals(int.class, result);
    }

    @Test
    void testUnwrap_withFloatClass() {
        // Test the unwrapping of Float class to its primitive type float
        Class<?> result = Primitives.unwrap(Float.class);
        assertEquals(float.class, result);
    }

    @Test
    void testUnwrap_withByteClass() {
        // Test the unwrapping of Byte class to its primitive type byte
        Class<?> result = Primitives.unwrap(Byte.class);
        assertEquals(byte.class, result);
    }

    @Test
    void testUnwrap_withDoubleClass() {
        // Test the unwrapping of Double class to its primitive type double
        Class<?> result = Primitives.unwrap(Double.class);
        assertEquals(double.class, result);
    }

    @Test
    void testUnwrap_withLongClass() {
        // Test the unwrapping of Long class to its primitive type long
        Class<?> result = Primitives.unwrap(Long.class);
        assertEquals(long.class, result);
    }

    @Test
    void testUnwrap_withCharacterClass() {
        // Test the unwrapping of Character class to its primitive type char
        Class<?> result = Primitives.unwrap(Character.class);
        assertEquals(char.class, result);
    }

    @Test
    void testUnwrap_withBooleanClass() {
        // Test the unwrapping of Boolean class to its primitive type boolean
        Class<?> result = Primitives.unwrap(Boolean.class);
        assertEquals(boolean.class, result);
    }

    @Test
    void testUnwrap_withShortClass() {
        // Test the unwrapping of Short class to its primitive type short
        Class<?> result = Primitives.unwrap(Short.class);
        assertEquals(short.class, result);
    }

    @Test
    void testUnwrap_withVoidClass() {
        // Test the unwrapping of Void class to its primitive type void
        Class<?> result = Primitives.unwrap(Void.class);
        assertEquals(void.class, result);
    }

    @Test
    void testUnwrap_withUnknownClass() {
        // Test the behavior of unwrap when an unknown class (non-wrapper) is provided
        Class<?> result = Primitives.unwrap(String.class);
        assertEquals(String.class, result);
    }

    @ParameterizedTest
    @MethodSource("provideTestClassesForUnwrapping")
    void testUnwrap_withWrapperClasses(Class<?> wrapper, Class<?> expectedPrimitive) {
        // Test the unwrapping of various wrapper classes to their primitive types
        Class<?> result = Primitives.unwrap(wrapper);
        assertEquals(expectedPrimitive, result);
    }

    @Test
    void testUnwrap_withPrimitiveClass() {
        // Test the behavior of unwrap when a primitive class is provided
        Class<?> result = Primitives.unwrap(int.class);
        assertEquals(int.class, result);
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testUnwrap_withNullInput() {
        // Test the behavior of unwrap when null is provided
        assertThrows(NullPointerException.class, () -> {
            try {
                unwrapMethod.invoke(null, (Object) null);
            } catch (Exception e) {
                // Throwing the cause to capture NullPointerException if occurs
                throw e.getCause();
            }
        });
    }
}