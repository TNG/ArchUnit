package com.tngtech.archunit.core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.tngtech.archunit.base.DescribedPredicate;
import org.junit.Test;

import static com.tngtech.archunit.core.AccessTarget.Predicates.constructor;
import static com.tngtech.archunit.core.AccessTarget.Predicates.declaredIn;
import static com.tngtech.archunit.core.TestUtils.simulateCall;
import static com.tngtech.archunit.core.TestUtils.withinImportedClasses;
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
    public void predicate_declaredIn() {
        JavaCall<?> call = simulateCall().from(Origin.class, "call").to(Target.class, "called");

        assertThat(declaredIn(Target.class).apply(call.getTarget()))
                .as("predicate matches").isTrue();
        assertThat(declaredIn(Origin.class).apply(call.getTarget()))
                .as("predicate matches").isFalse();
        assertThat(declaredIn(Target.class).getDescription())
                .as("description").isEqualTo("declared in " + Target.class.getName());
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