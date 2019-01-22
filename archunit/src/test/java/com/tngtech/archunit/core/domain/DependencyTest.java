package com.tngtech.archunit.core.domain;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.google.common.base.MoreObjects;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.testutil.Assertions;
import com.tngtech.archunit.testutil.assertion.DependencyAssertion;
import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.domain.Dependency.Functions.GET_ORIGIN_CLASS;
import static com.tngtech.archunit.core.domain.Dependency.Functions.GET_TARGET_CLASS;
import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependency;
import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependencyOrigin;
import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependencyTarget;
import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.core.domain.TestUtils.importClassesWithContext;
import static com.tngtech.archunit.core.domain.TestUtils.simulateCall;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class DependencyTest {
    @Test
    public void Dependency_from_access() {
        JavaMethodCall call = simulateCall().from(getClass(), "toString").to(Object.class, "toString");

        Dependency dependency = Dependency.from(call);
        assertThat(dependency.getTargetClass()).as("target class").isEqualTo(call.getTargetOwner());
        assertThat(dependency.getDescription())
                .as("description").isEqualTo(call.getDescription());
    }

    @Test
    public void Dependency_from_origin_and_target() {
        JavaClass origin = importClassWithContext(getClass());
        JavaClass target = importClassWithContext(DependencyClass.class);
        Dependency dependency = createDependency(origin, target);
        assertThat(dependency.getDescription()).as("description")
                .contains("Class <" + origin.getName() + "> extends class <" + target.getName() + ">");

        target = importClassWithContext(DependencyInterface.class);
        dependency = createDependency(origin, target);
        assertThat(dependency.getDescription()).as("description")
                .contains("Class <" + origin.getName() + "> implements interface <" + target.getName() + ">");

        origin = importClassWithContext(DependencySubInterface.class);
        dependency = createDependency(origin, target);
        assertThat(dependency.getDescription()).as("description")
                .contains("Interface <" + origin.getName() + "> extends interface <" + target.getName() + ">");
    }

    @Test
    public void Dependency_from_throws_declaration() {
        JavaMethod origin = importClassesWithContext(ClassWithDependencyOnThrowable.class, IOException.class)
                .get(ClassWithDependencyOnThrowable.class).getMethod("method");
        ThrowsDeclaration<JavaMethod> throwsDeclaration = getOnlyElement(origin.getThrowsClause());

        Dependency dependency = Dependency.fromThrowsDeclaration(throwsDeclaration);

        assertThat(dependency.getOriginClass()).matches(ClassWithDependencyOnThrowable.class);
        assertThat(dependency.getTargetClass()).matches(IOException.class);
        assertThat(dependency.getDescription()).as("description")
                .contains("Method <" + origin.getFullName() + "> throws type <" + IOException.class.getName() + ">");
    }

    @Test
    public void Dependency_from_annotation_on_class() {
        JavaClass origin = importClassesWithContext(ClassWithDependencyOnAnnotation.class, SomeAnnotation.class)
                .get(ClassWithDependencyOnAnnotation.class);

        JavaAnnotation annotation = origin.getAnnotations().iterator().next();
        Class<?> annotationClass = annotation.getType().reflect();

        Dependency dependency = Dependency.fromAnnotation(origin, annotation);
        assertThat(dependency.getOriginClass()).matches(ClassWithDependencyOnAnnotation.class);
        assertThat(dependency.getTargetClass()).matches(annotationClass);
        assertThat(dependency.getDescription()).as("description")
                .contains("Class <" + origin.getName() + "> has annotation <" + annotationClass.getName() + ">");

        origin = importClassesWithContext(InterfaceWithDependencyOnAnnotation.class, SomeAnnotation.class)
                .get(InterfaceWithDependencyOnAnnotation.class);

        dependency = Dependency.fromAnnotation(origin, annotation);
        assertThat(dependency.getOriginClass()).matches(InterfaceWithDependencyOnAnnotation.class);
        assertThat(dependency.getTargetClass()).matches(annotationClass);
        assertThat(dependency.getDescription()).as("description")
                .contains("Interface <" + origin.getName() + "> has annotation <" + annotationClass.getName() + ">");
    }

    @Test
    public void Dependency_from_annotation_on_member() {
        JavaField origin = importClassesWithContext(ClassWithAnnotatedField.class, SomeAnnotation.class)
                .get(ClassWithAnnotatedField.class)
                .getField("obj");

        JavaAnnotation annotation = origin.getAnnotations().iterator().next();
        Class<?> annotationClass = annotation.getType().reflect();

        Dependency dependency = Dependency.fromAnnotation(origin, annotation);
        assertThat(dependency.getOriginClass()).matches(ClassWithAnnotatedField.class);
        assertThat(dependency.getTargetClass()).matches(annotationClass);
        assertThat(dependency.getDescription()).as("description")
                .contains(origin.getDescription() + " has annotation <" + annotationClass.getName() + ">");
    }

    @Test
    public void Dependency_from_class_annotation_member() {
        JavaClasses context = importClassesWithContext(Origin.class, Target.class);
        JavaClass origin = context.get(Origin.class);
        JavaClass target = context.get(Target.class);

        Dependency dependency = Dependency.fromAnnotationMember(origin, target);
        assertThat(dependency.getOriginClass()).matches(Origin.class);
        assertThat(dependency.getTargetClass()).matches(Target.class);
        assertThat(dependency.getDescription()).as("description")
                .contains("Class <" + origin.getName() + "> has annotation member of type <" + target.getName() + ">");
    }

    @Test
    public void origin_predicates_match() {
        assertThatDependency(Origin.class, Target.class)
                .matchesOrigin(Origin.class)
                .doesntMatchOrigin(Target.class);
    }

    @Test
    public void origin_predicates_descriptions() {
        assertThat(dependencyOrigin(Origin.class))
                .hasSameDescriptionAs(dependencyOrigin(Origin.class.getName()))
                .hasDescription("origin " + Origin.class.getName());

        assertThat(dependencyOrigin(predicateWithDescription("foo")))
                .hasDescription("origin foo");
    }

    @Test
    public void target_predicates_match() {
        assertThatDependency(Origin.class, Target.class)
                .matchesTarget(Target.class)
                .doesntMatchTarget(Origin.class);
    }

    @Test
    public void target_predicates_descriptions() {
        assertThat(dependencyTarget(Target.class))
                .hasSameDescriptionAs(dependencyTarget(Target.class.getName()))
                .hasDescription("target " + Target.class.getName());

        assertThat(dependencyTarget(predicateWithDescription("foo")))
                .hasDescription("target foo");
    }

    @Test
    public void dependency_predicates_match() {
        assertThatDependency(Origin.class, Target.class)
                .matches(Origin.class, Target.class)
                .doesntMatch(Origin.class, Origin.class)
                .doesntMatch(Target.class, Target.class);
    }

    @Test
    public void dependency_predicates_descriptions() {
        assertThat(dependency(Origin.class, Target.class))
                .hasSameDescriptionAs(dependency(Origin.class.getName(), Target.class.getName()))
                .hasDescription("dependency " + Origin.class.getName() + " -> " + Target.class.getName());

        assertThat(dependency(predicateWithDescription("first"), predicateWithDescription("second")))
                .hasDescription("dependency first -> second");
    }

    @Test
    public void functions() {
        assertThat(GET_ORIGIN_CLASS.apply(createDependency(Origin.class, Target.class))).matches(Origin.class);
        assertThat(GET_TARGET_CLASS.apply(createDependency(Origin.class, Target.class))).matches(Target.class);
    }

    private Dependency createDependency(JavaClass origin, JavaClass target) {
        Dependency dependency = Dependency.fromInheritance(origin, target);
        assertThat(dependency.getOriginClass()).as("origin class").isEqualTo(origin);
        assertThat(dependency.getTargetClass()).as("target class").isEqualTo(target);
        return dependency;
    }

    private DescribedPredicate<JavaClass> predicateWithDescription(String description) {
        return DescribedPredicate.<JavaClass>alwaysTrue().as(description);
    }

    private static DependencyAssertion assertThatDependency(Class<?> originClass, Class<?> targetClass) {
        return Assertions.assertThatDependency(createDependency(originClass, targetClass));
    }

    private static Dependency createDependency(Class<?> originClass, Class<?> targetClass) {
        return Dependency.fromInheritance(
                importClassWithContext(originClass), importClassWithContext(targetClass));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).toString();
    }

    private static class DependencyClass {
    }

    private interface DependencySubInterface extends DependencyInterface {
    }

    private interface DependencyInterface {
    }

    private static class Origin {
    }

    private static class Target {
    }

    @SuppressWarnings({"RedundantThrows", "unused"})
    private static class ClassWithDependencyOnThrowable {
        void method() throws IOException {
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface SomeAnnotation {}

    @SomeAnnotation
    private static class ClassWithDependencyOnAnnotation { }

    @SomeAnnotation
    private interface InterfaceWithDependencyOnAnnotation { }

    private static class ClassWithAnnotatedField {
        @SomeAnnotation private Object obj;
    }

}
