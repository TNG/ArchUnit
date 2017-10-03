package com.tngtech.archunit.core.domain.properties;

import java.lang.annotation.Retention;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import org.junit.Rule;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

public class CanBeAnnotatedTest {
    @Test
    public void matches_annotation_by_type() {
        assertThat(annotatedWith(SomeAnnotation.class).apply(importClassWithContext(AnnotatedClass.class)))
                .as("annotated class matches").isTrue();
        assertThat(annotatedWith(SomeAnnotation.class.getName()).apply(importClassWithContext(AnnotatedClass.class)))
                .as("annotated class matches").isTrue();

        assertThat(annotatedWith(SomeAnnotation.class).apply(importClassWithContext(Object.class)))
                .as("annotated class matches").isFalse();
        assertThat(annotatedWith(SomeAnnotation.class.getName()).apply(importClassWithContext(Object.class)))
                .as("annotated class matches").isFalse();

        assertThat(annotatedWith(Rule.class).getDescription())
                .isEqualTo("annotated with @Rule");
        assertThat(annotatedWith(Rule.class.getName()).getDescription())
                .isEqualTo("annotated with @Rule");
    }

    @Test
    public void matches_annotation_by_predicate() {
        assertThat(annotatedWith(DescribedPredicate.<JavaAnnotation>alwaysTrue())
                .apply(importClassWithContext(AnnotatedClass.class)))
                .as("annotated class matches").isTrue();
        assertThat(annotatedWith(DescribedPredicate.<JavaAnnotation>alwaysFalse())
                .apply(importClassWithContext(AnnotatedClass.class)))
                .as("annotated class matches").isFalse();

        assertThat(annotatedWith(DescribedPredicate.<JavaAnnotation>alwaysTrue().as("Something")).getDescription())
                .isEqualTo("annotated with Something");
    }

    @Retention(RUNTIME)
    @interface SomeAnnotation {
    }

    @SomeAnnotation
    private static class AnnotatedClass {
    }
}