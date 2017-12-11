package com.tngtech.archunit.core.domain.properties;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.tngtech.archunit.base.ArchUnitException.InvalidSyntaxUsageException;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

public class CanBeAnnotatedTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void matches_annotation_by_type() {
        assertThat(annotatedWith(RuntimeRetentionAnnotation.class).apply(importClassWithContext(AnnotatedClass.class)))
                .as("annotated class matches").isTrue();
        assertThat(annotatedWith(RuntimeRetentionAnnotation.class.getName()).apply(importClassWithContext(AnnotatedClass.class)))
                .as("annotated class matches").isTrue();

        assertThat(annotatedWith(RuntimeRetentionAnnotation.class).apply(importClassWithContext(Object.class)))
                .as("annotated class matches").isFalse();
        assertThat(annotatedWith(RuntimeRetentionAnnotation.class.getName()).apply(importClassWithContext(Object.class)))
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

    /**
     * Since ArchUnit checks bytecode, it's very likely wrong API usage to look for an annotation with @Retention(SOURCE)
     */
    @Test
    public void annotatedWith_Retention_Source_is_rejected() {
        annotatedWith(RuntimeRetentionAnnotation.class);
        annotatedWith(ClassRetentionAnnotation.class);
        annotatedWith(DefaultClassRetentionAnnotation.class);

        expectInvalidSyntaxUsageForRetentionSource(thrown);
        annotatedWith(SourceRetentionAnnotation.class);
    }

    public static void expectInvalidSyntaxUsageForRetentionSource(ExpectedException thrown) {
        thrown.expect(InvalidSyntaxUsageException.class);
        thrown.expectMessage(Retention.class.getSimpleName());
        thrown.expectMessage(RetentionPolicy.SOURCE.name());
        thrown.expectMessage("useless");
    }

    @Retention(RUNTIME)
    public @interface RuntimeRetentionAnnotation {
    }

    @Retention(RetentionPolicy.CLASS)
    public @interface ClassRetentionAnnotation {
    }

    public @interface DefaultClassRetentionAnnotation {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface SourceRetentionAnnotation {
    }

    @RuntimeRetentionAnnotation
    private static class AnnotatedClass {
    }
}