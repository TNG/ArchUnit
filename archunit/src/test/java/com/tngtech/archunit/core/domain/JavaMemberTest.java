package com.tngtech.archunit.core.domain;

import java.lang.annotation.Retention;

import com.tngtech.archunit.base.DescribedPredicate;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaMember.Predicates.declaredIn;
import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.core.domain.TestUtils.importClassesWithContext;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class JavaMemberTest {
    @Test
    public void isAnnotatedWith_type() {
        assertThat(importField(SomeClass.class, "someField").isAnnotatedWith(Deprecated.class))
                .as("field is annotated with @Deprecated").isTrue();
        assertThat(importField(SomeClass.class, "someField").isAnnotatedWith(Retention.class))
                .as("field is annotated with @Retention").isFalse();
    }

    @Test
    public void isAnnotatedWith_typeName() {
        assertThat(importField(SomeClass.class, "someField").isAnnotatedWith(Deprecated.class.getName()))
                .as("field is annotated with @Deprecated").isTrue();
        assertThat(importField(SomeClass.class, "someField").isAnnotatedWith(Retention.class.getName()))
                .as("field is annotated with @Retention").isFalse();
    }

    @Test
    public void isAnnotatedWith_predicate() {
        assertThat(importField(SomeClass.class, "someField")
                .isAnnotatedWith(DescribedPredicate.<JavaAnnotation<?>>alwaysTrue()))
                .as("predicate matches").isTrue();
        assertThat(importField(SomeClass.class, "someField")
                .isAnnotatedWith(DescribedPredicate.<JavaAnnotation<?>>alwaysFalse()))
                .as("predicate matches").isFalse();
    }

    @Test
    public void isMetaAnnotatedWith_type() {
        JavaClass clazz = importClassesWithContext(SomeClass.class, Deprecated.class).get(SomeClass.class);

        assertThat(clazz.getField("someField").isMetaAnnotatedWith(Deprecated.class))
                .as("field is meta-annotated with @Deprecated").isFalse();
        assertThat(clazz.getField("someField").isMetaAnnotatedWith(Retention.class))
                .as("field is meta-annotated with @Retention").isTrue();
    }

    @Test
    public void isMetaAnnotatedWith_typeName() {
        JavaClass clazz = importClassesWithContext(SomeClass.class, Deprecated.class).get(SomeClass.class);

        assertThat(clazz.getField("someField").isMetaAnnotatedWith(Deprecated.class.getName()))
                .as("field is meta-annotated with @Deprecated").isFalse();
        assertThat(clazz.getField("someField").isMetaAnnotatedWith(Retention.class.getName()))
                .as("field is meta-annotated with @Retention").isTrue();
    }

    @Test
    public void isMetaAnnotatedWith_predicate() {
        JavaClass clazz = importClassesWithContext(SomeClass.class, Deprecated.class).get(SomeClass.class);

        assertThat(clazz.getField("someField")
                .isMetaAnnotatedWith(DescribedPredicate.<JavaAnnotation<?>>alwaysTrue()))
                .as("predicate matches").isTrue();
        assertThat(clazz.getField("someField")
                .isMetaAnnotatedWith(DescribedPredicate.<JavaAnnotation<?>>alwaysFalse()))
                .as("predicate matches").isFalse();
    }

    @Test
    public void predicate_declaredIn() {
        JavaField field = importField(SomeClass.class, "someField");

        assertThat(declaredIn(SomeClass.class))
                .accepts(field)
                .hasDescription("declared in " + SomeClass.class.getName());
        assertThat(declaredIn(SomeClass.class.getName()))
                .accepts(field)
                .hasDescription("declared in " + SomeClass.class.getName());
        assertThat(declaredIn(equivalentTo(SomeClass.class).as("custom")))
                .accepts(field)
                .hasDescription("declared in custom");

        assertThat(declaredIn(getClass())).rejects(field);
        assertThat(declaredIn(getClass().getName())).rejects(field);
        assertThat(declaredIn(equivalentTo(getClass()))).rejects(field);
    }

    private static JavaField importField(Class<?> owner, String name) {
        return importClassWithContext(owner).getField(name);
    }

    private static class SomeClass {
        @Deprecated
        private String someField;
    }
}
