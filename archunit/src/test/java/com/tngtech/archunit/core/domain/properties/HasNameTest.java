package com.tngtech.archunit.core.domain.properties;

import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameEndingWith;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class HasNameTest {
    @Test
    public void match_against_regex() {
        assertMatches("Some.foobar", ".*foo.*").isTrue();
        assertMatches("Some.fobar", ".*foo.*").isFalse();
        assertMatches("Some.foobar", ".*fob?o*.*").isTrue();
        assertMatches("com.tngtech.SomeClass", ".*W.*").isFalse();
        assertMatches("com.tngtech.SomeClass", "com.*").isTrue();
        assertMatches("com.tngtech.SomeClass", "co\\..*").isFalse();
        assertMatches("com.tngtech.SomeClass", ".*Class").isTrue();
        assertMatches("com.tngtech.SomeClass", ".*Clas").isFalse();
        assertMatches("com.tngtech.SomeClass", ".*\\.S.*s").isTrue();

        assertThat(nameMatching(".*foo").getDescription()).isEqualTo("name matching '.*foo'");
    }

    @Test
    public void match_against_name() {
        assertThat(name("some.Foo").apply(newHasName("some.Foo"))).isTrue();
        assertThat(name("some.Foo").apply(newHasName("some.Fo"))).isFalse();
        assertThat(name("Foo").apply(newHasName("some.Foo"))).isFalse();

        assertThat(name("some.Foo").getDescription()).isEqualTo("name 'some.Foo'");
    }

    @Test
    public void match_against_suffix() {
        HasName input = newHasName("some.Foo");
        assertThat(nameEndingWith(".Foo").apply(input)).isTrue();
        // Full match test
        assertThat(nameEndingWith("some.Foo").apply(input)).isTrue();
        assertThat(nameEndingWith("").apply(input)).isTrue();
        assertThat(nameEndingWith(" ").apply(input)).isFalse();

        assertThat(nameEndingWith(".Fo").apply(input)).isFalse();
        assertThat(nameEndingWith("some.Fo").apply(input)).isFalse();

        assertThat(nameEndingWith("some.Foo").getDescription()).isEqualTo("name ending with 'some.Foo'");
    }

    private AbstractBooleanAssert assertMatches(String input, String regex) {
        return assertThat(nameMatching(regex).apply(newHasName(input)))
                .as(input + " =~ " + regex);
    }

    private HasName newHasName(final String name) {
        return new HasName() {
            @Override
            public String getName() {
                return name;
            }
        };
    }
}