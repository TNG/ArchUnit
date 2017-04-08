package com.tngtech.archunit.core;

import java.lang.annotation.Retention;

import com.tngtech.archunit.base.DescribedPredicate;
import org.junit.Test;

import static com.tngtech.archunit.core.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.JavaMember.Predicates.declaredIn;
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

    @Test
    public void predicate_declaredIn() {
        JavaField field = javaFieldViaReflection(SomeClass.class, "someField");

        assertThat(declaredIn(SomeClass.class).apply(field))
                .as("predicate matches").isTrue();
        assertThat(declaredIn(SomeClass.class.getName()).apply(field))
                .as("predicate matches").isTrue();
        assertThat(declaredIn(equivalentTo(SomeClass.class)).apply(field))
                .as("predicate matches").isTrue();

        assertThat(declaredIn(getClass()).apply(field))
                .as("predicate matches").isFalse();
        assertThat(declaredIn(getClass().getName()).apply(field))
                .as("predicate matches").isFalse();
        assertThat(declaredIn(equivalentTo(getClass())).apply(field))
                .as("predicate matches").isFalse();

        assertThat(declaredIn(SomeClass.class).getDescription())
                .as("description").isEqualTo("declared in " + SomeClass.class.getName());
        assertThat(declaredIn(SomeClass.class.getName()).getDescription())
                .as("description").isEqualTo("declared in " + SomeClass.class.getName());
        assertThat(declaredIn(DescribedPredicate.<JavaClass>alwaysTrue().as("custom")).getDescription())
                .as("description").isEqualTo("declared in custom");
    }

    private static class SomeClass {
        @Deprecated
        private String someField;
    }
}