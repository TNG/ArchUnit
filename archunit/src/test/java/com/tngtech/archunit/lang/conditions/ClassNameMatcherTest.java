package com.tngtech.archunit.lang.conditions;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class ClassNameMatcherTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void exact_match() {
        assertThat(ClassNameMatcher.of("SomeClass").matches("SomeClass"))
                .as("simple class name matches")
                .isTrue();
    }

    @Test
    public void simple_mismatch() {
        assertThat(ClassNameMatcher.of("SomeOther").matches("SomeClass"))
                .as("simple class name matches")
                .isFalse();
    }

    @Test
    public void null_identifier_is_rejected() {
        thrown.expect(IllegalArgumentException.class);

        ClassNameMatcher.of(null);
    }

    @Test
    public void null_matching_is_rejected() {
        ClassNameMatcher matcher = ClassNameMatcher.of("SomeClass");

        thrown.expect(IllegalArgumentException.class);

        matcher.matches(null);
    }

    @Test
    public void wildcard_start() {
        assertThat(ClassNameMatcher.of("*Class").matches("SomeClass"))
                .as("simple class name matches")
                .isTrue();
    }

    @Test
    public void wildcard_end() {
        assertThat(ClassNameMatcher.of("Some*").matches("SomeClass"))
                .as("simple class name matches")
                .isTrue();
    }

    @Test
    public void multiple_wildcards() {
        assertThat(ClassNameMatcher.of("S*eC*s*").matches("SomeClass"))
                .as("simple class name matches")
                .isTrue();
    }

    @Test
    public void multiple_wildcards_mismatch() {
        assertThat(ClassNameMatcher.of("S*aC*s*").matches("SomeClass"))
                .as("simple class name matches")
                .isFalse();
    }

    @Test
    public void respects_case_sensitivity() {
        assertThat(ClassNameMatcher.of("S*ec*s*").matches("SomeClass"))
                .as("simple class name matches")
                .isFalse();
    }
}