package com.tngtech.archunit.core.domain.properties;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.tngtech.archunit.base.ArchUnitException.InvalidSyntaxUsageException;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.core.domain.TestUtils.importClassesWithContext;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.metaAnnotatedWith;
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

    @Test
    public void matches_meta_annotation_by_type() {
        JavaClasses classes = importClassesWithContext(MetaAnnotatedClass.class, Object.class, MetaRuntimeRetentionAnnotation.class);

        assertThat(metaAnnotatedWith(RuntimeRetentionAnnotation.class).apply(classes.get(MetaAnnotatedClass.class)))
                .as("meta-annotated class matches").isTrue();
        assertThat(metaAnnotatedWith(RuntimeRetentionAnnotation.class.getName()).apply(classes.get(MetaAnnotatedClass.class)))
                .as("meta-annotated class matches").isTrue();

        assertThat(metaAnnotatedWith(RuntimeRetentionAnnotation.class).apply(classes.get(Object.class)))
                .as("meta-annotated class matches").isFalse();
        assertThat(metaAnnotatedWith(RuntimeRetentionAnnotation.class.getName()).apply(classes.get(Object.class)))
                .as("meta-annotated class matches").isFalse();

        assertThat(metaAnnotatedWith(Rule.class).getDescription())
                .isEqualTo("meta-annotated with @Rule");
        assertThat(metaAnnotatedWith(Rule.class.getName()).getDescription())
                .isEqualTo("meta-annotated with @Rule");
    }

    @Test
    public void matches_meta_annotation_by_predicate() {
        JavaClass clazz = importClassesWithContext(MetaAnnotatedClass.class, MetaRuntimeRetentionAnnotation.class).get(MetaAnnotatedClass.class);

        assertThat(metaAnnotatedWith(DescribedPredicate.<JavaAnnotation>alwaysTrue()).apply(clazz))
                .as("meta-annotated class matches").isTrue();
        assertThat(metaAnnotatedWith(DescribedPredicate.<JavaAnnotation>alwaysFalse()).apply(clazz))
                .as("meta-annotated class matches").isFalse();

        assertThat(metaAnnotatedWith(DescribedPredicate.<JavaAnnotation>alwaysTrue().as("Something")).getDescription())
                .isEqualTo("meta-annotated with Something");
    }

    /**
     * Since ArchUnit checks bytecode, it's very likely wrong API usage to look for an annotation with @Retention(SOURCE)
     */
    @Test
    public void metaAnnotatedWith_Retention_Source_is_rejected() {
        metaAnnotatedWith(RuntimeRetentionAnnotation.class);
        metaAnnotatedWith(ClassRetentionAnnotation.class);
        metaAnnotatedWith(DefaultClassRetentionAnnotation.class);

        expectInvalidSyntaxUsageForRetentionSource(thrown);
        metaAnnotatedWith(SourceRetentionAnnotation.class);
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

    @Retention(RUNTIME)
    @RuntimeRetentionAnnotation
    public @interface MetaRuntimeRetentionAnnotation {
    }

    @MetaRuntimeRetentionAnnotation
    private static class MetaAnnotatedClass {
    }
}