package com.tngtech.archunit.lang.conditions;

import java.lang.annotation.Retention;

import com.tngtech.archunit.core.JavaClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.tngtech.archunit.core.TestUtils.javaClass;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.annotatedWith;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.inTheHierarchyOfAClassThat;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.named;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.resideIn;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ArchPredicatesTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private JavaClass mockClass;

    @Test
    public void matches_class_package() {
        when(mockClass.getPackage()).thenReturn("some.arbitrary.pkg");

        assertThat(resideIn("some..pkg").apply(mockClass)).as("package matches").isTrue();
    }

    @Test
    public void mismatches_class_package() {
        when(mockClass.getPackage()).thenReturn("wrong.arbitrary.pkg");

        assertThat(resideIn("some..pkg").apply(mockClass)).as("package matches").isFalse();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void matches_annotation() {
        when(mockClass.reflect()).thenReturn((Class) AnnotatedClass.class);

        assertThat(annotatedWith(SomeAnnotation.class).apply(mockClass))
                .as("annotated class matches")
                .isTrue();

        when(mockClass.reflect()).thenReturn((Class) NotAnnotatedClass.class);

        assertThat(annotatedWith(SomeAnnotation.class).apply(mockClass))
                .as("annotated class matches")
                .isFalse();
    }

    @Test
    public void matches_name() {
        when(mockClass.getSimpleName()).thenReturn("SomeClass");

        assertThat(named(".*Class").apply(mockClass)).as("class name matches").isTrue();
        assertThat(named(".*Wrong").apply(mockClass)).as("class name matches").isFalse();
        assertThat(named("Some.*").apply(mockClass)).as("class name matches").isTrue();
        assertThat(named("Wrong.*").apply(mockClass)).as("class name matches").isFalse();
        assertThat(named("S.*s").apply(mockClass)).as("class name matches").isTrue();
        assertThat(named("W.*").apply(mockClass)).as("class name matches").isFalse();
    }

    @Test
    public void inTheHierarchyOfAClass_matches_class_itself() {
        assertThat(inTheHierarchyOfAClassThat(named(".*Class")).apply(javaClass(AnnotatedClass.class)))
                .as("class itself matches the predicate").isTrue();
    }

    @Test
    public void inTheHierarchyOfAClass_matches_subclass() {
        assertThat(inTheHierarchyOfAClassThat(named("Annotated.*")).apply(javaClass(SubClass.class)))
                .as("subclass matches the predicate").isTrue();
    }

    @Test
    public void inTheHierarchyOfAClass_does_not_match_superclass() {
        assertThat(inTheHierarchyOfAClassThat(named("Annotated.*")).apply(javaClass(Object.class)))
                .as("superclass matches the predicate").isFalse();
    }

    @Retention(RUNTIME)
    @interface SomeAnnotation {
    }

    @SomeAnnotation
    public static class AnnotatedClass {
    }

    static class SubClass extends AnnotatedClass {
    }

    public static class NotAnnotatedClass {
    }
}