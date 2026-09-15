package com.google.gson.internal;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.*;
import java.util.Collection;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class $Gson$Types_equals_Test {

    private Type type1;

    private Type type2;

    private Type type3;

    @BeforeEach
    void setupBeforeEach() {
        // Initialize the types for the tests
        type1 = String.class;
        type2 = String.class;
        type3 = Integer.class;
    }

    @Test
    void testEquals_withSameClassTypes() {
        // Both types are the same (String.class), ensures that equality is true.
        assertTrue($Gson$Types.equals(type1, type2), "Strings should be equal");
    }

    @Test
    void testEquals_withDifferentClassTypes() {
        // Comparing String.class with Integer.class, ensures that equality is false.
        assertFalse($Gson$Types.equals(type1, type3), "String and Integer should not be equal");
    }

    @Test
    void testEquals_withNulls() {
        // Both types are null, should be considered equal.
        assertTrue($Gson$Types.equals(null, null), "Two null types are equal");
        // One type is null while the other is not, should not be equal.
        assertFalse($Gson$Types.equals(type1, null), "String should not equal null");
        assertFalse($Gson$Types.equals(null, type1), "Null should not equal String");
    }

    @Test
    void testEquals_withParameterizedTypes() {
        // Creating mock parameterized types that are equal.
        ParameterizedType paramType1 = mock(ParameterizedType.class);
        ParameterizedType paramType2 = mock(ParameterizedType.class);
        when(paramType1.getRawType()).thenReturn(Collection.class);
        when(paramType2.getRawType()).thenReturn(Collection.class);
        when(paramType1.getActualTypeArguments()).thenReturn(new Type[] { String.class });
        when(paramType2.getActualTypeArguments()).thenReturn(new Type[] { String.class });
        // Asserting that equal parameterized types return true.
        assertTrue($Gson$Types.equals(paramType1, paramType2), "Parameterized types should be equal");
    }

    @Test
    void testEquals_withDifferentParameterizedTypes() {
        // Creating mock parameterized types that are different.
        ParameterizedType paramType1 = mock(ParameterizedType.class);
        ParameterizedType paramType2 = mock(ParameterizedType.class);
        when(paramType1.getRawType()).thenReturn(Collection.class);
        when(paramType2.getRawType()).thenReturn(Collection.class);
        when(paramType1.getActualTypeArguments()).thenReturn(new Type[] { String.class });
        when(paramType2.getActualTypeArguments()).thenReturn(new Type[] { Integer.class });
        // Asserting that different parameterized types return false.
        assertFalse($Gson$Types.equals(paramType1, paramType2), "Parameterized types with different arguments should not be equal");
    }

    @Test
    void testEquals_withGenericArrayTypes() throws Exception {
        // Creating mock generic array types that are equal.
        GenericArrayType genericArrayType1 = mock(GenericArrayType.class);
        GenericArrayType genericArrayType2 = mock(GenericArrayType.class);
        when(genericArrayType1.getGenericComponentType()).thenReturn(String.class);
        when(genericArrayType2.getGenericComponentType()).thenReturn(String.class);
        // Asserting that equal generic array types return true.
        assertTrue($Gson$Types.equals(genericArrayType1, genericArrayType2), "Generic array types should be equal");
    }

    @Test
    void testEquals_withDifferentGenericArrayTypes() throws Exception {
        // Creating mock generic array types that are different.
        GenericArrayType genericArrayType1 = mock(GenericArrayType.class);
        GenericArrayType genericArrayType2 = mock(GenericArrayType.class);
        when(genericArrayType1.getGenericComponentType()).thenReturn(String.class);
        when(genericArrayType2.getGenericComponentType()).thenReturn(Integer.class);
        // Asserting that different generic array types return false.
        assertFalse($Gson$Types.equals(genericArrayType1, genericArrayType2), "Different generic array types should not be equal");
    }

    @Test
    void testEquals_withWildcardTypes() throws Exception {
        // Creating mock wildcard types that are equal.
        WildcardType wildcardType1 = mock(WildcardType.class);
        WildcardType wildcardType2 = mock(WildcardType.class);
        when(wildcardType1.getUpperBounds()).thenReturn(new Type[] { String.class });
        when(wildcardType2.getUpperBounds()).thenReturn(new Type[] { String.class });
        // Asserting that equal wildcard types return true.
        assertTrue($Gson$Types.equals(wildcardType1, wildcardType2), "Wildcard types should be equal");
    }

    @Test
    void testEquals_withDifferentWildcardTypes() throws Exception {
        // Creating mock wildcard types that are different.
        WildcardType wildcardType1 = mock(WildcardType.class);
        WildcardType wildcardType2 = mock(WildcardType.class);
        when(wildcardType1.getUpperBounds()).thenReturn(new Type[] { String.class });
        when(wildcardType2.getUpperBounds()).thenReturn(new Type[] { Integer.class });
        // Asserting that different wildcard types return false.
        assertFalse($Gson$Types.equals(wildcardType1, wildcardType2), "Different wildcard types should not be equal");
    }

    @Test
    void testEquals_withTypeVariables() throws Exception {
        // Creating mock type variables that are equal.
        TypeVariable<?> typeVar1 = mock(TypeVariable.class);
        TypeVariable<?> typeVar2 = mock(TypeVariable.class);
        when(typeVar1.getName()).thenReturn("T");
        when(typeVar2.getName()).thenReturn("T");
        when(typeVar1.getGenericDeclaration()).thenReturn(String.class);
        when(typeVar2.getGenericDeclaration()).thenReturn(String.class);
        // Asserting that equal type variables return true.
        assertTrue($Gson$Types.equals(typeVar1, typeVar2), "Type variables should be equal");
    }

    @Test
    void testEquals_withDifferentTypeVariables() throws Exception {
        // Creating mock type variables that are different.
        TypeVariable<?> typeVar1 = mock(TypeVariable.class);
        TypeVariable<?> typeVar2 = mock(TypeVariable.class);
        when(typeVar1.getName()).thenReturn("T");
        when(typeVar2.getName()).thenReturn("S");
        when(typeVar1.getGenericDeclaration()).thenReturn(String.class);
        when(typeVar2.getGenericDeclaration()).thenReturn(String.class);
        // Asserting that different type variables return false.
        assertFalse($Gson$Types.equals(typeVar1, typeVar2), "Different type variables should not be equal");
    }

    @Test
    void testEquals_withUnknownTypes() {
        // Testing with types that are not instances of known types
        // Using Type rather than Object
        Type unknownType1 = new Object() {
        }.getClass();
        Type unknownType2 = new Object() {
        }.getClass();
        assertFalse($Gson$Types.equals(unknownType1, unknownType2), "Unknown types should not be equal");
    }

    @Test
    void testEquals_withOneNullAndOneUnknownType() {
        // Testing with one null and one unknown type
        Type unknownType = new Object() {
        }.getClass();
        assertFalse($Gson$Types.equals(null, unknownType), "Null should not equal unknown type");
    }
@Test
void test_equals_class_with_false_equals() {
    Class<?> class1 = String.class;
    Class<?> class2 = Integer.class;
    assertFalse($Gson$Types.equals(class1, class2), "Different classes should not be equal");
}
}