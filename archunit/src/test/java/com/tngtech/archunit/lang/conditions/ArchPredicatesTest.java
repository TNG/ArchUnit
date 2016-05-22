package com.tngtech.archunit.lang.conditions;

import java.lang.annotation.Retention;

import com.tngtech.archunit.core.JavaClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.tngtech.archunit.lang.conditions.ArchPredicates.annotatedWith;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.named;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.resideIn;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ArchPredicatesTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private JavaClass javaClass;

    @Test
    public void matches_class_package() {
        when(javaClass.getPackage()).thenReturn("some.arbitrary.pkg");

        assertThat(resideIn("some..pkg").apply(javaClass)).as("package matches").isTrue();
    }

    @Test
    public void mismatches_class_package() {
        when(javaClass.getPackage()).thenReturn("wrong.arbitrary.pkg");

        assertThat(resideIn("some..pkg").apply(javaClass)).as("package matches").isFalse();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void matches_annotation() {
        when(javaClass.reflect()).thenReturn((Class) AnnotatedClass.class);

        assertThat(annotatedWith(SomeAnnotation.class).apply(javaClass))
                .as("annotated class matches")
                .isTrue();

        when(javaClass.reflect()).thenReturn((Class) NotAnnotatedClass.class);

        assertThat(annotatedWith(SomeAnnotation.class).apply(javaClass))
                .as("annotated class matches")
                .isFalse();
    }

    @Test
    public void matches_name() {
        when(javaClass.getSimpleName()).thenReturn("SomeClass");

        assertThat(named("*Class").apply(javaClass)).as("class name matches").isTrue();
        assertThat(named("*Wrong").apply(javaClass)).as("class name matches").isFalse();
    }

    @Retention(RUNTIME)
    @interface SomeAnnotation {
    }

    @SomeAnnotation
    public static class AnnotatedClass {
    }

    public static class NotAnnotatedClass {
    }
}