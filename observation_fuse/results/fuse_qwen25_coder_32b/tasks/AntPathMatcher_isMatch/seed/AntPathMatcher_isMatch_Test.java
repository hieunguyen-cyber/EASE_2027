package org.flmelody.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.ParameterizedTest;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class AntPathMatcher_isMatch_Test {

    private AntPathMatcher antPathMatcher;

    @BeforeEach
    void setupBeforeEach() {
        // Initialize the builder and create an instance of AntPathMatcher
        AntPathMatcher.Builder builder = new AntPathMatcher.Builder();
        antPathMatcher = builder.withIgnoreCase().withPathSeparator('/').withTrimTokens().build();
    }

    @Test
    void testIsMatchWithVariousPatterns() {
        assertTrue(antPathMatcher.isMatch("/api/**", "/api/test"), "Pattern /api/** should match path /api/test");
        assertTrue(antPathMatcher.isMatch("/**", "/api/test"), "Pattern /** should match path /api/test");
        assertTrue(antPathMatcher.isMatch("/**/test", "/api/test"), "Pattern /**/test should match path /api/test");
        assertTrue(antPathMatcher.isMatch("/api/**.js", "/api/test.js"), "Pattern /api/**.js should match path /api/test.js");
        assertFalse(antPathMatcher.isMatch("/api/**.js", "/api/test"), "Pattern /api/**.js should not match path /api/test");
        assertTrue(antPathMatcher.isMatch("/api/**.js", "/api/test/a.js"), "Pattern /api/**.js should match path /api/test/a.js");
        assertFalse(antPathMatcher.isMatch("/static/**.js", "/api/test/a.js"), "Pattern /static/**.js should not match path /api/test/a.js");
    }

    @Test
    void testIsMatchWithEmptyPatternAndPath() {
        assertTrue(antPathMatcher.isMatch("", ""), "Empty pattern should match empty path");
    }

    @Test
    void testIsMatchWithEmptyPatternNonEmptyPath() {
        assertFalse(antPathMatcher.isMatch("", "/api/test"), "Empty pattern should not match non-empty path");
    }

//     @Test
//     void testIsMatchWithEmptyPathAndPatternWithSeparator() {
//         // Assume matchStart is true, so /api/ should match empty path
//         // Adjusting the AntPathMatcher to consider matching start, needs to be reflected in actual matcher logic
//         antPathMatcher = new AntPathMatcher.Builder().withMatchStart(true).withPathSeparator('/').build();
//         assertTrue(antPathMatcher.isMatch("/api/", ""), "Pattern /api/ should match empty path when matchStart is true");
//     }

    @Test
    void testIsMatchWithPatternWithAsterisk() {
        assertTrue(antPathMatcher.isMatch("/*", "/api"), "Pattern /* should match path /api");
        assertTrue(antPathMatcher.isMatch("/api/*", "/api/test"), "Pattern /api/* should match path /api/test");
        assertFalse(antPathMatcher.isMatch("/api/*", "/test/api"), "Pattern /api/* should not match path /test/api");
    }

    @Test
    void testIsMatchWithDoubleAsterisk() {
        assertTrue(antPathMatcher.isMatch("/api/**", "/api/test/1/2"), "Pattern /api/** should match deeper path /api/test/1/2");
    }

    @Test
    void testIsMatchWithQuestionMark() {
        assertTrue(antPathMatcher.isMatch("/api/?", "/api/t"), "Pattern /api/? should match path /api/t");
        assertFalse(antPathMatcher.isMatch("/api/?", "/api/test"), "Pattern /api/? should not match path /api/test");
    }

    @Test
    void testIsMatchWithPathSeparatorConfiguration() {
        antPathMatcher = new AntPathMatcher.Builder().withPathSeparator('\\').build();
        assertTrue(antPathMatcher.isMatch("\\api\\**", "\\api\\test"), "Pattern \\api\\** should match path \\api\\test");
    }

    @ParameterizedTest
    @CsvSource({ "/path/to/file.html, /path/to/file.html, true", "/path/to/**, /path/to/file.html, true", "/path/to/**, /path/to/, true", "/path/to/*.html, /path/to/file.html, true", "/path/to/*.html, /path/to/file.js, false", "/*, /, true", "/*, /path, false", "/**/*.html, /path/to/file.html, true", "/**/*.html, /path/to/file.js, false" })
    void testIsMatchWithParameterizedInputs(String pattern, String path, boolean expectedResult) {
        assertEquals(expectedResult, antPathMatcher.isMatch(pattern, path), "Pattern " + pattern + " should match path " + path + " correctly");
    }

    @Test
    void testIsMatchWithNullPattern() {
        assertThrows(NullPointerException.class, () -> {
            antPathMatcher.isMatch(null, "/api/test");
        }, "Pattern should not be null");
    }

    @Test
    void testIsMatchWithNullPath() {
        assertThrows(NullPointerException.class, () -> {
            antPathMatcher.isMatch("/api/test", null);
        }, "Path should not be null");
    }

    @Test
    void testIsMatchWithNullPatternAndNullPath() {
        assertThrows(NullPointerException.class, () -> {
            antPathMatcher.isMatch(null, null);
        }, "Both pattern and path should not be null");
    }

    @Test
    void testIsMatchWithEmptyPatternAndNullPath() {
        assertFalse(antPathMatcher.isMatch("", null), "Empty pattern should not match null path");
    }

    @Test
    void testIsMatchWithNullPatternAndEmptyPath() {
        assertFalse(antPathMatcher.isMatch(null, ""), "Null pattern should not match empty path");
    }

    @Test
    void testIsMatchWithInvalidPatternCharacters() {
        assertFalse(antPathMatcher.isMatch("!@#$%^&*", "/api/test"), "Pattern with invalid characters should not match");
    }
}