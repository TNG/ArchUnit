package com.tngtech.archunit.core.domain;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget.CodeUnitCallTarget;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.AccessTarget.Predicates.constructor;
import static com.tngtech.archunit.core.domain.AccessTarget.Predicates.declaredIn;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.TestUtils.importClassesWithContext;
import static com.tngtech.archunit.core.domain.TestUtils.simulateCall;
import static com.tngtech.archunit.core.domain.TestUtils.withinImportedClasses;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

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

        assertThat(call.getTarget().isAnnotatedWith(DescribedPredicate.<JavaAnnotation<?>>alwaysTrue()))
                .as("target is annotated with anything")
                .isTrue();
        assertThat(call.getTarget().isAnnotatedWith(DescribedPredicate.<JavaAnnotation<?>>alwaysFalse()))
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
        assertThat(call.getTarget().isAnnotatedWith(DescribedPredicate.<JavaAnnotation<?>>alwaysTrue()))
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

        assertThat(call.getTarget().isMetaAnnotatedWith(DescribedPredicate.<JavaAnnotation<?>>alwaysTrue()))
                .as("target is meta-annotated with anything")
                .isTrue();
        assertThat(call.getTarget().isMetaAnnotatedWith(DescribedPredicate.<JavaAnnotation<?>>alwaysFalse()))
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
        assertThat(call.getTarget().isMetaAnnotatedWith(DescribedPredicate.<JavaAnnotation<?>>alwaysTrue()))
                .as("target is meta-annotated with anything")
                .isFalse();
    }

    @Test
    public void no_throws_clause_is_resolved() {
        CodeUnitCallTarget target = getTarget("withoutThrowsDeclaration");

        ThrowsClause<CodeUnitCallTarget> throwsClause = target.getThrowsClause();
        assertThat(throwsClause).as("throws clause").isEmpty();
        assertThat(throwsClause.getTypes()).isEmpty();
        assertThat(throwsClause.getOwner()).isEqualTo(target);
        assertThat(throwsClause.getDeclaringClass()).matches(Target.class);
    }

    @Test
    public void single_throws_declaration_is_resolved() {
        CodeUnitCallTarget target = getTarget("withASingleThrowsDeclaration");

        assertDeclarations(target, FirstCheckedException.class);
    }

    @Test
    public void multiple_throws_declarations_are_resolved() {
        CodeUnitCallTarget target = getTarget("withMultipleThrowsDeclarations");

        assertDeclarations(target, FirstCheckedException.class, SecondCheckedException.class);
    }

    @Test
    public void throws_declarations_on_non_unique_call_Targets_are_intersected() {
        CodeUnitCallTarget target = getTarget("diamondMethod");

        assertDeclarations(target, SecondCheckedException.class, ThirdCheckedException.class);
    }

    @Test
    public void predicate_declaredIn() {
        JavaCall<?> call = simulateCall().from(Origin.class, "call").to(Target.class, "called");

        assertThat(declaredIn(Target.class))
                .accepts(call.getTarget())
                .hasDescription("declared in " + Target.class.getName());
        assertThat(declaredIn(Target.class.getName()))
                .accepts(call.getTarget())
                .hasDescription("declared in " + Target.class.getName());
        assertThat(declaredIn(equivalentTo(Target.class).as("custom")))
                .accepts(call.getTarget())
                .hasDescription("declared in custom");

        assertThat(declaredIn(Origin.class))
                .rejects(call.getTarget());
        assertThat(declaredIn(Origin.class.getName()))
                .rejects(call.getTarget());
        assertThat(declaredIn(equivalentTo(Origin.class)))
                .rejects(call.getTarget());
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

        assertThat(constructor())
                .accepts(constructorCall.getTarget())
                .rejects(methodCall.getTarget())
                .hasDescription("constructor");
    }

    private void assertDeclarations(CodeUnitCallTarget target, Class<?>... exceptionTypes) {
        ThrowsClause<CodeUnitCallTarget> throwsClause = target.getThrowsClause();
        assertThat(throwsClause.getTypes()).matches(exceptionTypes);
        for (ThrowsDeclaration<CodeUnitCallTarget> throwsDeclaration : throwsClause) {
            assertThat(throwsDeclaration.getDeclaringClass()).isEqualTo(target.getOwner());
            assertThat(throwsDeclaration.getOwner()).isEqualTo(target.getThrowsClause());
            assertThat(throwsDeclaration.getLocation()).isEqualTo(target);
        }
    }

    private CodeUnitCallTarget getTarget(String targetName) {
        JavaClass origin = importClassesWithContext(Origin.class, Target.class).get(Origin.class);
        return getTarget(origin, targetName);
    }

    private CodeUnitCallTarget getTarget(JavaClass javaClass, String targetName) {
        for (JavaCall<?> call : javaClass.getCallsFromSelf()) {
            if (call.getTarget().getName().equals(targetName)) {
                return call.getTarget();
            }
        }
        throw new AssertionError(String.format("Couldn't find target %s.%s", javaClass.getSimpleName(), targetName));
    }

    @SuppressWarnings("unused")
    private static class Origin {
        private Target target;
        private C c;

        void call() throws Exception {
            target = new Target();
            target.called();
            target.withoutThrowsDeclaration();
            target.withASingleThrowsDeclaration();
            target.withMultipleThrowsDeclarations();
        }

        void callDiamond() {
            try {
                c.diamondMethod();
            } catch (SecondCheckedException | ThirdCheckedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @SuppressWarnings("RedundantThrows")
    private static class Target {
        Target() {
        }

        @QueriedAnnotation
        void called() {
        }

        void withoutThrowsDeclaration() {
        }

        void withASingleThrowsDeclaration() throws FirstCheckedException {
        }

        void withMultipleThrowsDeclarations() throws FirstCheckedException, SecondCheckedException {
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface QueriedAnnotation {
    }

    private interface A {
        void diamondMethod() throws FirstCheckedException, SecondCheckedException, ThirdCheckedException;
    }

    @SuppressWarnings("unused")
    private interface B {
        void diamondMethod() throws SecondCheckedException, ThirdCheckedException, FourthCheckedException;
    }

    private interface C extends A, B {
    }

    private static class FirstCheckedException extends Exception {
    }

    private static class SecondCheckedException extends Exception {
    }

    private static class ThirdCheckedException extends Exception {
    }

    private static class FourthCheckedException extends Exception {
    }
}
