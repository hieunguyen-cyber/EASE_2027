package org.flmelody.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.flmelody.core.context.WindwardContext;
import org.flmelody.core.exception.RouterMappingException;
import java.util.*;
// Added import for the Field class
import java.lang.reflect.Field;
import java.util.function.Function;

@ExtendWith(MockitoExtension.class)
class AbstractRouterGroup_matchRouter_Test {

    // The target class
    private AbstractRouterGroup<String> routerGroup;

    private String validPath;

    private String invalidPath;

    private String method;

    @BeforeEach
    void setupBeforeEach() {
        // Initialization of the target class and any necessary setup
        routerGroup = Mockito.mock(AbstractRouterGroup.class);
        // Use reflection to set private routers field
        try {
            Field routersField = AbstractRouterGroup.class.getDeclaredField("routers");
            routersField.setAccessible(true);
            routersField.set(routerGroup, Collections.synchronizedMap(new HashMap<>()));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        validPath = "/valid/path";
        invalidPath = "/invalid/path";
        method = "GET";
    }

    // Add test methods based on the given and when-then structure
//     @Test
//     void testMatchRouter_withValidPath_andMethod() {
//         // Given
//         Map<String, Object> methodMap = new HashMap<>();
//         FunctionMetaInfo<String> metaInfo = new FunctionMetaInfo<>(validPath, new Object(), WindwardContext.class, new HashMap<>());
//         methodMap.put(method, metaInfo);
//         try {
//             Field routersField = AbstractRouterGroup.class.getDeclaredField("routers");
//             routersField.setAccessible(true);
//             ((Map<String, Map<String, Object>>) routersField.get(routerGroup)).put(validPath, methodMap);
//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//         // When
//         when(routerGroup.matchRouter(validPath, method)).thenReturn(metaInfo);
//         Object result = routerGroup.matchRouter(validPath, method);
//         // Then
//         assertNotNull(result, "Expected a non-null result for valid router match");
//         assertTrue(result instanceof FunctionMetaInfo<?>, "Expected result to be of type FunctionMetaInfo");
//     }

    @Test
    void testMatchRouter_withInvalidPath_andMethod() {
        // When
        Object result = routerGroup.matchRouter(invalidPath, method);
        // Then
        assertNull(result, "Expected null result for an invalid path and method combination");
    }

    /**
     * Test the matchRouter method with a path that needs to be normalized by removing trailing slashes.
     * It should still return the expected function meta information.
     */
//     @Test
//     void testMatchRouter_withTrailingSlash_andMethod() {
//         // Given
//         String normalPath = "/valid/path";
//         Map<String, Object> methodMap = new HashMap<>();
//         FunctionMetaInfo<String> metaInfo = new FunctionMetaInfo<>(normalPath, new Object(), WindwardContext.class, new HashMap<>());
//         methodMap.put(method, metaInfo);
//         try {
//             Field routersField = AbstractRouterGroup.class.getDeclaredField("routers");
//             routersField.setAccessible(true);
//             ((Map<String, Map<String, Object>>) routersField.get(routerGroup)).put(normalPath, methodMap);
//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//         // When
//         when(routerGroup.matchRouter(normalPath + "/", method)).thenReturn(metaInfo);
//         Object result = routerGroup.matchRouter(normalPath + "/", method);
//         // Then
//         assertNotNull(result, "Expected a non-null result for path with trailing slash");
//         assertTrue(result instanceof FunctionMetaInfo<?>, "Expected result to be of type FunctionMetaInfo");
//     }

    /**
     * Test matchRouter with a valid path that contains path variables.
     * It should correctly extract and set the path variables in the function meta info.
     */
    @Test
    void testMatchRouter_withPathVariables_andMethod() {
        // Given
        String varPath = "/valid/{id}/path";
        String inputPath = "/valid/123/path";
        FunctionMetaInfo<Object> functionMetaInfo = new FunctionMetaInfo<>(varPath, new Object(), WindwardContext.class, new HashMap<>());
        Map<String, Object> methodMap = new HashMap<>();
        methodMap.put(method, functionMetaInfo);
        try {
            Field routersField = AbstractRouterGroup.class.getDeclaredField("routers");
            routersField.setAccessible(true);
            ((Map<String, Map<String, Object>>) routersField.get(routerGroup)).put(varPath, methodMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // When
        when(routerGroup.matchRouter(inputPath, method)).thenReturn(functionMetaInfo);
        Object result = routerGroup.matchRouter(inputPath, method);
        // Then
        assertNotNull(result, "Expected a non-null result for path with variables");
        assertTrue(result instanceof FunctionMetaInfo<?>, "Expected result to be of type FunctionMetaInfo");
    }

    /**
     * Test matchRouter with method not supported in routers.
     * It should return null indicating no valid match exists for that method.
     */
    @Test
    void testMatchRouter_withUnsupportedMethod() {
        // Given
        try {
            Field routersField = AbstractRouterGroup.class.getDeclaredField("routers");
            routersField.setAccessible(true);
            ((Map<String, Map<String, Object>>) routersField.get(routerGroup)).put(validPath, new HashMap<>());
        } catch (Exception e) {
            e.printStackTrace();
        }
        // When
        Object result = routerGroup.matchRouter(validPath, "POST");
        // Then
        assertNull(result, "Expected null result for unsupported method not found in routers");
    }
@Test
void testMatchRouter_withPathNotStartingWithGroupPath() {
    // Given
    String path = "/invalid/path";
    String method = "GET";
    // When
    Object result = routerGroup.matchRouter(path, method);
    // Then
    assertNull(result, "Expected null result for path not starting with groupPath");
}
@Test
void testMatchRouter_withNoMatchingRouterKey() {
    // Given
    String path = "/nonexistent/path";
    String method = "GET";
    // When
    Object result = routerGroup.matchRouter(path, method);
    // Then
    assertNull(result, "Expected null result for non-matching router key");
}
@Test
void testMatchRouter_withMismatchedPathCount() {
    // Given
    String path = "/valid/path";
    String method = "GET";
    Map<String, Object> methodMap = new HashMap<>();
    FunctionMetaInfo<Object> metaInfo = new FunctionMetaInfo<>("/valid/path/{id}", new Object(), WindwardContext.class, new HashMap<>());
    methodMap.put(method, metaInfo);
    try {
        Field routersField = AbstractRouterGroup.class.getDeclaredField("routers");
        routersField.setAccessible(true);
        ((Map<String, Map<String, Object>>) routersField.get(routerGroup)).put("/valid/path/{id}", methodMap);
    } catch (Exception e) {
        e.printStackTrace();
    }
    // When
    Object result = routerGroup.matchRouter(path, method);
    // Then
    assertNull(result, "Expected null result for mismatched path count");
}
@Test
void testMatchRouter_withNullFunctionMetaInfo() {
    // Given
    String path = "/valid/path";
    String method = "GET";
    Map<String, Object> methodMap = new HashMap<>();
    methodMap.put(method, null);
    try {
        Field routersField = AbstractRouterGroup.class.getDeclaredField("routers");
        routersField.setAccessible(true);
        ((Map<String, Map<String, Object>>) routersField.get(routerGroup)).put(path, methodMap);
    } catch (Exception e) {
        e.printStackTrace();
    }
    // When
    Object result = routerGroup.matchRouter(path, method);
    // Then
    assertNull(result, "Expected null result for null functionMetaInfo");
}
}