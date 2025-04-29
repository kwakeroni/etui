package com.quaxantis.support.util;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(SoftAssertionsExtension.class)
class GlobMatcherTest {

    @ParameterizedTest
    @DisplayName("Detects glob patterns in filename")
    @CsvSource({
            "my-file.txt,   false",
            "my?file.txt,   true",
            "my\\?file.txt, false",
            "*.txt,         true",
            "\\*.txt,       false",
            "**/,           true",
            "*\\*/,         true",
            "\\*\\*/,       false",
            "ab[c-f]gh,     true",
            "ab\\[c-f]gh,   false",
            "ab{cd}ef,      true",
            "ab\\{cd}ef,    false",
    })
    void testGlobPatternsInFilename(String filename, boolean expectedResult) {
        assertThat(GlobMatcher.hasGlobPattern(filename)).isEqualTo(expectedResult); // ?*{[
    }

    static List<Arguments> testOfArguments() {
        return List.of(
                arguments("C:/test", "C:/test", List.of("C:/test/a.xml", "C:/test/nested/b.xml"), List.of()),
                arguments("C:\\test", "C:/test", List.of("C:/test/a.xml", "C:/test/nested/b.xml"), List.of()),
                arguments("C:/test/a.xml", "C:/test/a.xml", List.of("C:/test/a.xml"), List.of()),
                arguments("C:\\test\\a.xml", "C:\\test\\a.xml", List.of("C:\\test\\a.xml"), List.of()),
                arguments("C:/test/*.xml", "C:/test",
                          List.of("C:/test/a.xml", "C:/test/b.xml"),
                          List.of("C:/a.xml", "C:/test/nested/b.xml", "C:/other/c.xml")),
                arguments("C:\\test\\*.xml", "C:/test",
                          List.of("C:/test/a.xml", "C:/test/b.xml"),
                          List.of("C:/a.xml", "C:/test/nested/b.xml", "C:/other/c.xml")),
                arguments("C:/test/**", "C:/test",
                          List.of("C:/test/a.xml", "C:/test/a.txt", "C:/test/nested/b.xml", "C:/test/nested/b.txt"),
                          List.of("C:/a.xml", "C:/other/a.xml")),
                arguments("C:\\test\\**", "C:/test",
                          List.of("C:/test/a.xml", "C:/test/a.txt", "C:/test/nested/b.xml", "C:/test/nested/b.txt"),
                          List.of("C:/a.xml", "C:/other/a.xml")),
                arguments("C:/**/*.xml", "C:/",
                          List.of("C:/test/a.xml", "C:/test/nested/b.xml"),
                          List.of("C:/a.xml", "C:/test/a.txt", "C:/test/nested/b.txt")),
                arguments("C:\\**\\*.xml", "C:/",
                          List.of("C:/test/a.xml", "C:/test/nested/b.xml"),
                          List.of("C:/a.xml", "C:/test/a.txt", "C:/test/nested/b.txt")),
                arguments("C:/test/**/*.xml", "C:/test",
                          List.of("C:/test/nested/b.xml"),
                          List.of("C:/a.xml", "C:/test/a.xml", "C:/test/a.txt", "C:/test/nested/b.txt", "C:/other/a.xml")),
                arguments("C:\\test\\**\\*.xml", "C:/test",
                          List.of("C:/test/nested/b.xml"),
                          List.of("C:/a.xml", "C:/test/a.xml", "C:/test/a.txt", "C:/test/nested/b.txt", "C:/other/a.xml")),
                arguments("C:/test/**.xml", "C:/test",
                          List.of("C:/test/a.xml", "C:/test/nested/b.xml"),
                          List.of("C:/a.xml", "C:/test/a.txt", "C:/test/nested/b.txt", "C:/other/a.xml")),
                arguments("C:\\test\\**.xml", "C:/test",
                          List.of("C:/test/a.xml", "C:/test/nested/b.xml"),
                          List.of("C:/a.xml", "C:/test/a.txt", "C:/test/nested/b.txt", "C:/other/a.xml"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testOfArguments")
    @DisplayName("Extracts a Path-PathMatcher pair from a path pattern")
    void testOf(String pathPattern, String expectedRoot, Collection<String> matchingPaths, Collection<String> nonMatchingPaths, SoftAssertions softly) {
        softly.assertThat("A").isNotNull();

        GlobMatcher globMatcher = GlobMatcher.of(pathPattern, "**.xml");
        assertThat(globMatcher).isNotNull();
        assertThat(globMatcher.path()).isEqualTo(Path.of(expectedRoot));

        for (String matchingPath : matchingPaths) {
            softly.assertThat(Path.of(matchingPath))
                    .matches(globMatcher::matches, "to match the glob " + pathPattern);
        }

        for (String matchingPath : nonMatchingPaths) {
            softly.assertThat(Path.of(matchingPath))
                    .matches(not(globMatcher::matches), "not to match the glob " + pathPattern);
        }

    }
}
