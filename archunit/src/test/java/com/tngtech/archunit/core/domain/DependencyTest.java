package com.tngtech.archunit.core.domain;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.google.common.base.MoreObjects;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.testutil.Assertions;
import com.tngtech.archunit.testutil.assertion.DependencyAssertion;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

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
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;

@RunWith(DataProviderRunner.class)
public class DependencyTest {
    @Test
    public void Dependency_from_access() {
        JavaMethodCall call = simulateCall().from(getClass(), "toString").to(Object.class, "toString");

        Dependency dependency = Dependency.tryCreateFromAccess(call).get();
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

        Dependency dependency = Dependency.tryCreateFromThrowsDeclaration(throwsDeclaration).get();

        assertThat(dependency.getOriginClass()).matches(ClassWithDependencyOnThrowable.class);
        assertThat(dependency.getTargetClass()).matches(IOException.class);
        assertThat(dependency.getDescription()).as("description")
                .contains("Method <" + origin.getFullName() + "> throws type <" + IOException.class.getName() + ">");
    }

    @DataProvider
    public static Object[][] annotated_classes() {
        JavaClasses classes = importClassesWithContext(
                ClassWithDependencyOnAnnotation.class, InterfaceWithDependencyOnAnnotation.class, SomeAnnotation.class, SomeMemberType.class);

        return testForEach(
                classes.get(ClassWithDependencyOnAnnotation.class),
                classes.get(InterfaceWithDependencyOnAnnotation.class));
    }

    @Test
    @UseDataProvider("annotated_classes")
    public void Dependency_from_annotation_on_class(JavaClass annotatedClass) {
        JavaAnnotation<?> annotation = annotatedClass.getAnnotations().iterator().next();
        Class<?> annotationClass = annotation.getRawType().reflect();

        Dependency dependency = Dependency.tryCreateFromAnnotation(annotation).get();
        assertThat(dependency.getOriginClass()).isEqualTo(annotatedClass);
        assertThat(dependency.getTargetClass()).matches(annotationClass);
        assertThat(dependency.getDescription()).as("description")
                .contains("Class <" + annotatedClass.getName() + "> is annotated with <" + annotationClass.getName() + ">");
    }

    @DataProvider
    public static Object[][] annotated_members() {
        JavaClass javaClass = importClassesWithContext(ClassWithAnnotatedMembers.class, SomeAnnotation.class, SomeMemberType.class)
                .get(ClassWithAnnotatedMembers.class);

        return testForEach(
                javaClass.getField("annotatedField"),
                javaClass.getConstructor(Object.class),
                javaClass.getMethod("annotatedMethod"));
    }

    @Test
    @UseDataProvider("annotated_members")
    public void Dependency_from_annotation_on_member(JavaMember annotatedMember) {
        JavaAnnotation<?> annotation = annotatedMember.getAnnotations().iterator().next();
        Class<?> annotationClass = annotation.getRawType().reflect();

        Dependency dependency = Dependency.tryCreateFromAnnotation(annotation).get();
        assertThat(dependency.getOriginClass()).matches(ClassWithAnnotatedMembers.class);
        assertThat(dependency.getTargetClass()).matches(annotationClass);
        assertThat(dependency.getDescription()).as("description")
                .contains(annotatedMember.getDescription() + " is annotated with <" + annotationClass.getName() + ">");
    }

    @Test
    @UseDataProvider("annotated_classes")
    public void Dependency_from_class_annotation_member(JavaClass annotatedClass) {
        JavaAnnotation<?> annotation = annotatedClass.getAnnotationOfType(SomeAnnotation.class.getName());
        JavaClass memberType = ((JavaClass) annotation.get("value").get());

        Dependency dependency = Dependency.tryCreateFromAnnotationMember(annotation, memberType).get();
        assertThat(dependency.getOriginClass()).isEqualTo(annotatedClass);
        assertThat(dependency.getTargetClass()).isEqualTo(memberType);
        assertThat(dependency.getDescription()).as("description")
                .contains("Class <" + annotatedClass.getName() + "> has annotation member of type <" + memberType.getName() + ">");
    }

    @Test
    @UseDataProvider("annotated_members")
    public void Dependency_from_member_annotation_member(JavaMember annotatedMember) {
        JavaAnnotation<?> annotation = annotatedMember.getAnnotationOfType(SomeAnnotation.class.getName());
        JavaClass memberType = ((JavaClass) annotation.get("value").get());

        Dependency dependency = Dependency.tryCreateFromAnnotationMember(annotation, memberType).get();
        assertThat(dependency.getOriginClass()).isEqualTo(annotatedMember.getOwner());
        assertThat(dependency.getTargetClass()).isEqualTo(memberType);
        assertThat(dependency.getDescription()).as("description")
                .contains(annotatedMember.getDescription() + " has annotation member of type <" + memberType.getName() + ">");
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

    private static class SomeMemberType {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface SomeAnnotation {
        Class<?> value();
    }

    @SomeAnnotation(SomeMemberType.class)
    private static class ClassWithDependencyOnAnnotation { }

    @SomeAnnotation(SomeMemberType.class)
    private interface InterfaceWithDependencyOnAnnotation { }

    @SuppressWarnings("unused")
    private static class ClassWithAnnotatedMembers {
        @SomeAnnotation(SomeMemberType.class)
        private Object annotatedField;

        @SomeAnnotation(SomeMemberType.class)
        public ClassWithAnnotatedMembers(Object annotatedField) {
            this.annotatedField = annotatedField;
        }

        @SomeAnnotation(SomeMemberType.class)
        void annotatedMethod() {
        }
    }
}
