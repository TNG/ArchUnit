package com.tngtech.archunit.core.domain.properties;

import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.simpleClassNameEndingWith;
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
        HasName.AndSimpleName input = newHasName("some.Foo");
        assertThat(simpleClassNameEndingWith(".Foo").apply(input)).isTrue();
        assertThat(simpleClassNameEndingWith("Foo").apply(input)).isTrue();
        assertThat(simpleClassNameEndingWith("").apply(input)).isTrue();

        // Full match test
        assertThat(simpleClassNameEndingWith("some.Foo").apply(input)).isFalse();
        assertThat(simpleClassNameEndingWith(" ").apply(input)).isFalse();
        assertThat(simpleClassNameEndingWith(".").apply(input)).isFalse();

        assertThat(simpleClassNameEndingWith(".Fo").apply(input)).isFalse();
        assertThat(simpleClassNameEndingWith("some.Fo").apply(input)).isFalse();

        assertThat(simpleClassNameEndingWith("some.Foo").getDescription()).isEqualTo("simple class name ending with 'some.Foo'");
    }

    private AbstractBooleanAssert assertMatches(String input, String regex) {
        return assertThat(nameMatching(regex).apply(newHasName(input)))
                .as(input + " =~ " + regex);
    }

    private HasName.AndSimpleName newHasName(final String name) {
        return new HasName.AndSimpleName() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getSimpleName() {
                int i = name.lastIndexOf('.');
                if (i == -1) {
                    return name;
                }

                return name.substring(i);
            }
        };
    }
}