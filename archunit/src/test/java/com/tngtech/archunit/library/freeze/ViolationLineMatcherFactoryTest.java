package com.tngtech.archunit.library.freeze;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class ViolationLineMatcherFactoryTest {

    private static Stream<Arguments> default_matcher() {
        return Stream.of(
                expectMatch(
                        "",
                        ""
                ),
                expectMatch(
                        "abc",
                        "abc"
                ),
                expectNoMatch(
                        "abc",
                        "abcd"
                ),
                expectMatch(
                        "(A.java:1)",
                        "(A.java:2)"
                ),
                expectNoMatch(
                        "A.java:1",
                        "A.java:2"
                ),
                expectNoMatch(
                        "(A.java:1)",
                        "(A.java:2"
                ),
                expectMatch(
                        "A$1 B$2 C$4 (X.java:111)",
                        "A$2 B$3 C$5 (X.java:222)"
                ),
                expectNoMatch(
                        "A$a",
                        "A$b"
                ),
                expectNoMatch(
                        "A:1",
                        "A$2"
                ),
                expectMatch(
                        "Method <MyClass.someSyntheticStuff$2()> has a violation in (MyClass.java:123)",
                        "Method <MyClass.someSyntheticStuff$123()> has a violation in (MyClass.java:0)"
                ),
                expectNoMatch(
                        "Method <C.someSyntheticStuff$200()> is bad in (C.java:123)",
                        "Method <C.someSyntheticStuff$200()> is bad in (C.java:123), too"
                ),
                expectMatch(  // limitation of the current implementation, false positive
                        "A:1) B$2 C",
                        "A: B$ C"
                )
        );
    }

    private static Arguments expectMatch(String str1, String str2) {
        return arguments(str1, str2, true);
    }

    private static Arguments expectNoMatch(String str1, String str2) {
        return arguments(str1, str2, false);
    }

    @ParameterizedTest
    @MethodSource
    void default_matcher(String str1, String str2, boolean expected) {
        ViolationLineMatcher defaultMatcher = ViolationLineMatcherFactory.create();
        assertThat(defaultMatcher.matches(str1, str2))
                .as(String.format("'%s' matches '%s'", str1, str2))
                .isEqualTo(expected);
    }
}
