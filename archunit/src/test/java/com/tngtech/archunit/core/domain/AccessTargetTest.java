package com.tngtech.archunit.core.domain;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.tngtech.archunit.base.DescribedPredicate;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.AccessTarget.Predicates.constructor;
import static com.tngtech.archunit.core.domain.AccessTarget.Predicates.declaredIn;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.TestUtils.importClassesWithContext;
import static com.tngtech.archunit.core.domain.TestUtils.simulateCall;
import static com.tngtech.archunit.core.domain.TestUtils.withinImportedClasses;
import static org.assertj.core.api.Assertions.assertThat;

public class AccessTargetTest {
    @Test
    public void isAnnotatedWith_type_on_resolved_target() {
        JavaCall<?> call = simulateCall().from(Origin.class, "call").to(Target.class, "called");

        assertThat(call.getTarget().isAnnotatedWith(QueriedAnnotation.class))
                .as("target is annotated with @" + QueriedAnnotation.class.getSimpleName())
                .isTrue();
        assertThat(call.getTarget().isAnnotatedWith(Deprecated.class))
                .as("target is annotated with @" + Deprecated.class.getSimpleName())
                .isFalse();
    }

    @Test
    public void isAnnotatedWith_typeName_on_resolved_target() {
        JavaCall<?> call = simulateCall().from(Origin.class, "call").to(Target.class, "called");

        assertThat(call.getTarget().isAnnotatedWith(QueriedAnnotation.class.getName()))
                .as("target is annotated with @" + QueriedAnnotation.class.getSimpleName())
                .isTrue();
        assertThat(call.getTarget().isAnnotatedWith(Deprecated.class.getName()))
                .as("target is annotated with @" + Deprecated.class.getSimpleName())
                .isFalse();
    }

    @Test
    public void isAnnotatedWith_predicate_on_resolved_target() {
        JavaCall<?> call = simulateCall().from(Origin.class, "call").to(Target.class, "called");

        assertThat(call.getTarget().isAnnotatedWith(DescribedPredicate.<JavaAnnotation>alwaysTrue()))
                .as("target is annotated with anything")
                .isTrue();
        assertThat(call.getTarget().isAnnotatedWith(DescribedPredicate.<JavaAnnotation>alwaysFalse()))
                .as("target is annotated with nothing")
                .isFalse();
    }

    @Test
    public void annotated_on_unresolved_target() {
        JavaCall<?> call = simulateCall().from(Origin.class, "call").toUnresolved(Target.class, "called");

        assertThat(call.getTarget().isAnnotatedWith(QueriedAnnotation.class))
                .as("target is annotated with @" + QueriedAnnotation.class.getSimpleName())
                .isFalse();
        assertThat(call.getTarget().isAnnotatedWith(QueriedAnnotation.class.getName()))
                .as("target is annotated with @" + QueriedAnnotation.class.getSimpleName())
                .isFalse();
        assertThat(call.getTarget().isAnnotatedWith(DescribedPredicate.<JavaAnnotation>alwaysTrue()))
                .as("target is annotated with anything")
                .isFalse();
    }

    @Test
    public void isMetaAnnotatedWith_type_on_resolved_target() {
        JavaClasses classes = importClassesWithContext(Origin.class, Target.class, QueriedAnnotation.class);
        JavaCall<?> call = simulateCall().from(classes.get(Origin.class), "call").to(classes.get(Target.class).getMethod("called"));

        assertThat(call.getTarget().isMetaAnnotatedWith(QueriedAnnotation.class))
                .as("target is meta-annotated with @" + QueriedAnnotation.class.getSimpleName())
                .isFalse();
        assertThat(call.getTarget().isMetaAnnotatedWith(Retention.class))
                .as("target is meta-annotated with @" + Retention.class.getSimpleName())
                .isTrue();
    }

    @Test
    public void isMetaAnnotatedWith_typeName_on_resolved_target() {
        JavaClasses classes = importClassesWithContext(Origin.class, Target.class, QueriedAnnotation.class);
        JavaCall<?> call = simulateCall().from(classes.get(Origin.class), "call").to(classes.get(Target.class).getMethod("called"));

        assertThat(call.getTarget().isMetaAnnotatedWith(QueriedAnnotation.class.getName()))
                .as("target is meta-annotated with @" + QueriedAnnotation.class.getSimpleName())
                .isFalse();
        assertThat(call.getTarget().isMetaAnnotatedWith(Retention.class.getName()))
                .as("target is meta-annotated with @" + Retention.class.getSimpleName())
                .isTrue();
    }

    @Test
    public void isMetaAnnotatedWith_predicate_on_resolved_target() {
        JavaClasses classes = importClassesWithContext(Origin.class, Target.class, QueriedAnnotation.class);
        JavaCall<?> call = simulateCall().from(classes.get(Origin.class), "call").to(classes.get(Target.class).getMethod("called"));

        assertThat(call.getTarget().isMetaAnnotatedWith(DescribedPredicate.<JavaAnnotation>alwaysTrue()))
                .as("target is meta-annotated with anything")
                .isTrue();
        assertThat(call.getTarget().isMetaAnnotatedWith(DescribedPredicate.<JavaAnnotation>alwaysFalse()))
                .as("target is meta-annotated with nothing")
                .isFalse();
    }

    @Test
    public void meta_annotated_on_unresolved_target() {
        JavaClasses classes = importClassesWithContext(Origin.class, Target.class, QueriedAnnotation.class);
        JavaCall<?> call = simulateCall().from(classes.get(Origin.class), "call").toUnresolved(Target.class, "called");

        assertThat(call.getTarget().isMetaAnnotatedWith(Retention.class))
                .as("target is meta-annotated with @" + Retention.class.getSimpleName())
                .isFalse();
        assertThat(call.getTarget().isMetaAnnotatedWith(Retention.class.getName()))
                .as("target is meta-annotated with @" + Retention.class.getSimpleName())
                .isFalse();
        assertThat(call.getTarget().isMetaAnnotatedWith(DescribedPredicate.<JavaAnnotation>alwaysTrue()))
                .as("target is meta-annotated with anything")
                .isFalse();
    }

    @Test
    public void predicate_declaredIn() {
        JavaCall<?> call = simulateCall().from(Origin.class, "call").to(Target.class, "called");

        assertThat(declaredIn(Target.class).apply(call.getTarget()))
                .as("predicate matches").isTrue();
        assertThat(declaredIn(Target.class.getName()).apply(call.getTarget()))
                .as("predicate matches").isTrue();
        assertThat(declaredIn(equivalentTo(Target.class)).apply(call.getTarget()))
                .as("predicate matches").isTrue();

        assertThat(declaredIn(Origin.class).apply(call.getTarget()))
                .as("predicate matches").isFalse();
        assertThat(declaredIn(Origin.class.getName()).apply(call.getTarget()))
                .as("predicate matches").isFalse();
        assertThat(declaredIn(equivalentTo(Origin.class)).apply(call.getTarget()))
                .as("predicate matches").isFalse();

        assertThat(declaredIn(Target.class).getDescription())
                .as("description").isEqualTo("declared in " + Target.class.getName());
        assertThat(declaredIn(Target.class.getName()).getDescription())
                .as("description").isEqualTo("declared in " + Target.class.getName());
        assertThat(declaredIn(DescribedPredicate.<JavaClass>alwaysTrue().as("custom")).getDescription())
                .as("description").isEqualTo("declared in custom");
    }

    @Test
    public void predicate_constructor() {
        JavaCall<?> constructorCall = withinImportedClasses(Origin.class, Target.class)
                .getCallFrom(Origin.class, "call")
                .toConstructor(Target.class);
        JavaCall<?> methodCall = withinImportedClasses(Origin.class, Target.class)
                .getCallFrom(Origin.class, "call")
                .toMethod(Target.class, "called");

        simulateCall().from(Origin.class, "call").to(Target.class, "called");

        assertThat(constructor().apply(constructorCall.getTarget()))
                .as("predicate matches").isTrue();
        assertThat(constructor().apply(methodCall.getTarget()))
                .as("predicate matches").isFalse();
        assertThat(constructor().getDescription())
                .as("description").isEqualTo("constructor");
    }

    private static class Origin {
        private Target target;

        void call() {
            target = new Target();
            target.called();
        }
    }

    private static class Target {
        Target() {
        }

        @QueriedAnnotation
        void called() {
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface QueriedAnnotation {
    }
}