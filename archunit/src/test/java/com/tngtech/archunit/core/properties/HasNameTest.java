package com.tngtech.archunit.core.properties;

import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.Test;

import static com.tngtech.archunit.core.properties.HasName.Predicates.withName;
import static com.tngtech.archunit.core.properties.HasName.Predicates.withNameMatching;
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

        assertThat(withNameMatching(".*foo").getDescription()).isEqualTo("with name matching '.*foo'");
    }

    @Test
    public void match_against_name() {
        assertThat(withName("some.Foo").apply(newHasName("some.Foo"))).isTrue();
        assertThat(withName("some.Foo").apply(newHasName("some.Fo"))).isFalse();
        assertThat(withName("Foo").apply(newHasName("some.Foo"))).isFalse();

        assertThat(withName("some.Foo").getDescription()).isEqualTo("with name 'some.Foo'");
    }

    private AbstractBooleanAssert assertMatches(String input, String regex) {
        return assertThat(withNameMatching(regex).apply(newHasName(input)))
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