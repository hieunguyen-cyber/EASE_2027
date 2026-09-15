package com.google.gson.reflect;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class TypeToken_isAssignableFrom_Test {

    private ParameterizedType paramTypeA;

    private ParameterizedType paramTypeB;

    private Map<String, Type> typeVarMap;

    private ParameterizedType paramTypeC;

    private ParameterizedType paramTypeD;

    private TypeToken<?> typeToken;

    @BeforeEach
    void setupBeforeEach() {
        paramTypeA = (ParameterizedType) TypeToken.getParameterized(List.class, String.class).getType();
        paramTypeB = (ParameterizedType) TypeToken.getParameterized(List.class, Object.class).getType();
        paramTypeC = (ParameterizedType) TypeToken.getParameterized(List.class, Integer.class).getType();
        paramTypeD = (ParameterizedType) TypeToken.getParameterized(List.class, Number.class).getType();
        typeVarMap = new HashMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up resources after each test
        paramTypeA = null;
        paramTypeB = null;
        paramTypeC = null;
        // Ensured that paramTypeD is also reset to null
        paramTypeD = null;
        typeVarMap = null;
    }

    // Utility method to create a wildcard type
    private ParameterizedType createWildcardTypeWithBounds(Class<?> clazz) {
        // Use TypeToken to create a wildcard type
        return (ParameterizedType) TypeToken.getParameterized(List.class, clazz).getType();
    }

    private boolean callIsAssignableFrom(Type fromType, ParameterizedType toType) {
        try {
            Method method = TypeToken.class.getDeclaredMethod("isAssignableFrom", Type.class, ParameterizedType.class, Map.class);
            method.setAccessible(true);
            return (boolean) method.invoke(null, fromType, toType, typeVarMap);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to invoke isAssignableFrom", e);
        }
    }

    @Test
    void testIsAssignableFromEqualTypes() {
        // Test when both parameterized types are the same
        assertTrue(callIsAssignableFrom(paramTypeA, paramTypeA));
    }

    @Test
    void testIsAssignableFromDifferentParameterizedTypes() {
        // Test when two different parameterized types are compared
        assertFalse(callIsAssignableFrom(paramTypeA, paramTypeB));
    }

    @Test
    void testIsAssignableFromWithNullFrom() {
        // Test with null as 'from' type
        assertFalse(callIsAssignableFrom(null, paramTypeA));
    }

    @Test
    void testIsAssignableFromWithSubstitutedTypeParameters() {
        // Test with a scenario where type parameters can be substituted to match
        TypeToken<List<String>> typeToken = (TypeToken<List<String>>) TypeToken.getParameterized(List.class, String.class);
        assertTrue(callIsAssignableFrom(paramTypeA, (ParameterizedType) typeToken.getType()));
    }

    @Test
    void testIsAssignableFromWithTypeVariableMapping() {
        // Test when the type variable mapping allows the assignment to be valid
        typeVarMap.put("E", String.class);
        TypeToken<List<String>> typeToken = (TypeToken<List<String>>) TypeToken.getParameterized(List.class, String.class);
        assertTrue(callIsAssignableFrom(paramTypeA, (ParameterizedType) typeToken.getType()));
    }

    @Test
    void testIsAssignableFromWithRawType() {
        // Test when comparing a parameterized type to its raw type
        assertTrue(callIsAssignableFrom(paramTypeA, (ParameterizedType) TypeToken.getParameterized(List.class).getType()));
    }

    @Test
    void testIsAssignableFromWithWildcard() {
        // Test case for handling wildcards
        ParameterizedType wildcardType = createWildcardTypeWithBounds(String.class);
        assertTrue(callIsAssignableFrom(paramTypeA, wildcardType));
    }

    @Test
    void testIsAssignableFromDifferentGenericTypes() {
        // Test that different generic types are not assignable
        assertFalse(callIsAssignableFrom(paramTypeC, paramTypeA));
    }

    @Test
    void testIsAssignableFromSelfReferencingGenericTypes() {
        // Test self-referencing generic types
        ParameterizedType selfRefType = (ParameterizedType) TypeToken.getParameterized(List.class, List.class).getType();
        assertTrue(callIsAssignableFrom(selfRefType, selfRefType));
    }
}
