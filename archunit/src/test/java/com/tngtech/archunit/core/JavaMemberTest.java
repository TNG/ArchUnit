package com.tngtech.archunit.core;

import java.lang.annotation.Retention;

import com.tngtech.archunit.base.DescribedPredicate;
import org.junit.Test;

import static com.tngtech.archunit.core.TestUtils.javaFieldViaReflection;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaMemberTest {
    @Test
    public void isAnnotatedWith_type() {
        assertThat(javaFieldViaReflection(SomeClass.class, "someField").isAnnotatedWith(Deprecated.class))
                .as("field is annotated with @Deprecated").isTrue();
        assertThat(javaFieldViaReflection(SomeClass.class, "someField").isAnnotatedWith(Retention.class))
                .as("field is annotated with @Retention").isFalse();
    }

    @Test
    public void isAnnotatedWith_typeName() {
        assertThat(javaFieldViaReflection(SomeClass.class, "someField").isAnnotatedWith(Deprecated.class.getName()))
                .as("field is annotated with @Deprecated").isTrue();
        assertThat(javaFieldViaReflection(SomeClass.class, "someField").isAnnotatedWith(Retention.class.getName()))
                .as("field is annotated with @Retention").isFalse();
    }

    @Test
    public void isAnnotatedWith_predicate() {
        assertThat(javaFieldViaReflection(SomeClass.class, "someField")
                .isAnnotatedWith(DescribedPredicate.<JavaAnnotation>alwaysTrue()))
                .as("predicate matches").isTrue();
        assertThat(javaFieldViaReflection(SomeClass.class, "someField")
                .isAnnotatedWith(DescribedPredicate.<JavaAnnotation>alwaysFalse()))
                .as("predicate matches").isFalse();
    }

    private static class SomeClass {
        @Deprecated
        private String someField;
    }
}