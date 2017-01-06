package com.tngtech.archunit.core.properties;

import java.lang.annotation.Retention;

import org.junit.Rule;
import org.junit.Test;

import static com.tngtech.archunit.core.TestUtils.javaClassViaReflection;
import static com.tngtech.archunit.core.properties.CanBeAnnotated.Predicates.annotatedWith;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

public class CanBeAnnotatedTest {
    @Test
    public void matches_annotation() {
        assertThat(annotatedWith(SomeAnnotation.class).apply(javaClassViaReflection(AnnotatedClass.class)))
                .as("annotated class matches")
                .isTrue();

        assertThat(annotatedWith(SomeAnnotation.class).apply(javaClassViaReflection(Object.class)))
                .as("annotated class matches")
                .isFalse();

        assertThat(annotatedWith(Rule.class).getDescription())
                .isEqualTo("annotated with @Rule");
    }

    @Retention(RUNTIME)
    @interface SomeAnnotation {
    }

    @SomeAnnotation
    private static class AnnotatedClass {
    }
}