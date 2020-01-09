package com.tngtech.archunit.core.domain.properties;

import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.properties.HasName.AndFullName.Functions.GET_FULL_NAME;
import static com.tngtech.archunit.core.domain.properties.HasName.AndFullName.Predicates.fullName;
import static com.tngtech.archunit.core.domain.properties.HasName.AndFullName.Predicates.fullNameMatching;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class HasNameTest {
    @Test
    public void nameMatching_predicate() {
        assertNameMatches("Some.foobar", ".*foo.*").isTrue();
        assertNameMatches("Some.fobar", ".*foo.*").isFalse();
        assertNameMatches("Some.foobar", ".*fob?o*.*").isTrue();
        assertNameMatches("com.tngtech.SomeClass", ".*W.*").isFalse();
        assertNameMatches("com.tngtech.SomeClass", "com.*").isTrue();
        assertNameMatches("com.tngtech.SomeClass", "co\\..*").isFalse();
        assertNameMatches("com.tngtech.SomeClass", ".*Class").isTrue();
        assertNameMatches("com.tngtech.SomeClass", ".*Clas").isFalse();
        assertNameMatches("com.tngtech.SomeClass", ".*\\.S.*s").isTrue();

        assertThat(nameMatching(".*foo")).hasDescription("name matching '.*foo'");
    }

    @Test
    public void name_predicate() {
        assertThat(name("some.Foo"))
                .accepts(newHasName("some.Foo"))
                .rejects(newHasName("some.Fo"))
                .hasDescription("name 'some.Foo'");
        assertThat(name("Foo")).rejects(newHasName("some.Foo"));
    }

    @Test
    public void fullName_predicate() {
        assertThat(fullName("some.Foo.field1"))
                .accepts(newHasNameAndFullName("field1", "some.Foo.field1"))
                .rejects(newHasNameAndFullName("field", "some.Foo.field"))
                .rejects(newHasNameAndFullName("field12", "some.Foo.field12"))
                .rejects(newHasNameAndFullName("some.Foo.field1", "some.Foo.field1.property"))
                .hasDescription("full name 'some.Foo.field1'");
    }

    @Test
    public void fullNameMatching_predicate() {
        assertThat(fullNameMatching(".*method\\(.*\\)"))
                .accepts(newHasNameAndFullName("method", "some.Foo.method(int)"))
                .accepts(newHasNameAndFullName("method", "some.Foo.method()"))
                .rejects(newHasNameAndFullName("method", "some.Foo.method"))
                .hasDescription("full name matching '.*method\\(.*\\)'");
    }

    @Test
    public void functions() {
        HasName.AndFullName test = newHasNameAndFullName("simple", "full");
        assertThat(GET_NAME.apply(test)).isEqualTo("simple");
        assertThat(GET_FULL_NAME.apply(test)).isEqualTo("full");
    }

    private AbstractBooleanAssert<?> assertNameMatches(String input, String regex) {
        return assertThat(nameMatching(regex).apply(newHasName(input)))
                .as(input + " =~ " + regex);
    }

    private HasName newHasName(final String name) {
        return newHasNameAndFullName(name, "full " + name);
    }

    private HasName.AndFullName newHasNameAndFullName(final String name, final String fullName) {
        return new HasName.AndFullName() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getFullName() {
                return fullName;
            }
        };
    }
}
