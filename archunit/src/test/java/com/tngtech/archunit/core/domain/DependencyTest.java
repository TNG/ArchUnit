package com.tngtech.archunit.core.domain;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.FileSystem;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.collect.FluentIterable;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.testobjects.ClassWithArrayDependencies;
import com.tngtech.archunit.core.domain.testobjects.ClassWithDependencyOnInstanceofCheck;
import com.tngtech.archunit.core.domain.testobjects.ClassWithDependencyOnInstanceofCheck.InstanceOfCheckTarget;
import com.tngtech.archunit.core.domain.testobjects.DependenciesOnClassObjects;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.testutil.Assertions;
import com.tngtech.archunit.testutil.assertion.DependenciesAssertion;
import com.tngtech.archunit.testutil.assertion.DependencyAssertion;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.base.Guava.toGuava;
import static com.tngtech.archunit.core.domain.Dependency.Functions.GET_ORIGIN_CLASS;
import static com.tngtech.archunit.core.domain.Dependency.Functions.GET_TARGET_CLASS;
import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependency;
import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependencyOrigin;
import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependencyTarget;
import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.core.domain.TestUtils.importClassesWithContext;
import static com.tngtech.archunit.core.domain.TestUtils.simulateCall;
import static com.tngtech.archunit.core.domain.properties.HasType.Predicates.rawType;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatDependencies;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.assertion.DependenciesAssertion.from;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;

@RunWith(DataProviderRunner.class)
public class DependencyTest {

    @DataProvider
    public static Object[][] field_array_types() throws NoSuchFieldException {
        @SuppressWarnings("unused")
        class ClassWithArrayDependencies {
            private String[] oneDimArray;
            private String[][] multiDimArray;
        }
        return testForEach(
                ClassWithArrayDependencies.class.getDeclaredField("oneDimArray"),
                ClassWithArrayDependencies.class.getDeclaredField("multiDimArray"));
    }

    @Test
    @UseDataProvider("field_array_types")
    public void Dependencies_from_field_with_component_type(Field reflectionArrayField) {
        Class<?> reflectionDeclaringClass = reflectionArrayField.getDeclaringClass();
        JavaField field = new ClassFileImporter().importClasses(reflectionDeclaringClass).get(reflectionDeclaringClass).getField(reflectionArrayField.getName());

        Set<Dependency> dependencies = Dependency.tryCreateFromField(field);

        DependenciesAssertion.ExpectedDependencies expectedDependencies = from(reflectionDeclaringClass).to(reflectionArrayField.getType())
                .withDescriptionContaining("Field <%s> has type <%s>", field.getFullName(), reflectionArrayField.getType().getName())
                .inLocation(DependencyTest.class, 0);
        Class<?> expectedComponentType = reflectionArrayField.getType().getComponentType();
        while (expectedComponentType != null) {
            expectedDependencies.from(reflectionDeclaringClass).to(expectedComponentType)
                    .withDescriptionContaining("Field <%s> depends on component type <%s>", field.getFullName(), expectedComponentType.getName())
                    .inLocation(DependencyTest.class, 0);
            expectedComponentType = expectedComponentType.getComponentType();
        }

        assertThatDependencies(dependencies).containOnly(expectedDependencies);
    }

    @Test
    public void Dependency_from_access() {
        JavaMethodCall call = simulateCall().from(getClass(), "toString").to(Object.class, "toString");

        Dependency dependency = getOnlyElement(Dependency.tryCreateFromAccess(call));
        assertThatType(dependency.getTargetClass()).as("target class").isEqualTo(call.getTargetOwner());
        assertThat(dependency.getDescription())
                .as("description").isEqualTo(call.getDescription());
    }

    @DataProvider
    public static Object[][] method_calls_to_array_types() {
        return $$(
                $(ClassWithArrayDependencies.class, "oneDimArray", String[].class, 6),
                $(ClassWithArrayDependencies.class, "multiDimArray", String[][].class, 10)
        );
    }

    @Test
    @UseDataProvider("method_calls_to_array_types")
    public void Dependency_from_access_with_component_type(Class<?> classDependingOnArray, String nameOfMethodWithArrayMethodCall, Class<?> arrayType, int expectedLineNumber) throws NoSuchMethodException {
        Method reflectionMethodWithArrayMethodCall = classDependingOnArray.getDeclaredMethod(nameOfMethodWithArrayMethodCall);
        Class<?> reflectionDeclaringClass = reflectionMethodWithArrayMethodCall.getDeclaringClass();
        JavaMethod method = new ClassFileImporter().importClasses(reflectionDeclaringClass)
                .get(reflectionDeclaringClass).getMethod(reflectionMethodWithArrayMethodCall.getName());
        JavaMethodCall call = getOnlyElement(method.getMethodCallsFromSelf());

        Set<Dependency> dependencies = Dependency.tryCreateFromAccess(call);

        DependenciesAssertion.ExpectedDependencies expectedDependencies = from(reflectionDeclaringClass).to(arrayType)
                .withDescriptionContaining("Method <%s> calls method <%s>", method.getFullName(), arrayType.getName() + ".clone()")
                .inLocation(classDependingOnArray, expectedLineNumber);
        Class<?> expectedComponentType = arrayType.getComponentType();
        while (expectedComponentType != null) {
            expectedDependencies.from(reflectionDeclaringClass).to(expectedComponentType)
                    .withDescriptionContaining("Method <%s> depends on component type <%s>", method.getFullName(), expectedComponentType.getName())
                    .inLocation(classDependingOnArray, expectedLineNumber);
            expectedComponentType = expectedComponentType.getComponentType();
        }

        assertThatDependencies(dependencies).containOnly(expectedDependencies);
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

        origin = importClassWithContext(DependencySubinterface.class);
        dependency = createDependency(origin, target);
        assertThat(dependency.getDescription()).as("description")
                .contains("Interface <" + origin.getName() + "> extends interface <" + target.getName() + ">");
    }

    @Test
    public void Dependency_from_throws_declaration() {
        JavaMethod origin = importClassesWithContext(ClassWithDependencyOnThrowable.class, IOException.class)
                .get(ClassWithDependencyOnThrowable.class).getMethod("method");
        ThrowsDeclaration<JavaMethod> throwsDeclaration = getOnlyElement(origin.getThrowsClause());

        Dependency dependency = getOnlyElement(Dependency.tryCreateFromThrowsDeclaration(throwsDeclaration));

        assertThatType(dependency.getOriginClass()).matches(ClassWithDependencyOnThrowable.class);
        assertThatType(dependency.getTargetClass()).matches(IOException.class);
        assertThat(dependency.getDescription()).as("description")
                .contains("Method <" + origin.getFullName() + "> throws type <" + IOException.class.getName() + ">");
    }

    @DataProvider
    public static Object[][] with_instanceof_check_members() {
        JavaClass javaClass = importClassesWithContext(ClassWithDependencyOnInstanceofCheck.class, InstanceOfCheckTarget.class)
                .get(ClassWithDependencyOnInstanceofCheck.class);

        return $$(
                $(javaClass.getStaticInitializer().get(), 6),
                $(javaClass.getConstructor(Object.class), 9),
                $(javaClass.getMethod("method", Object.class), 13));
    }

    @Test
    @UseDataProvider("with_instanceof_check_members")
    public void Dependency_from_instanceof_check_in_code_unit(JavaCodeUnit memberWithInstanceofCheck, int expectedLineNumber) {
        InstanceofCheck instanceofCheck = getOnlyElement(memberWithInstanceofCheck.getInstanceofChecks());

        Dependency dependency = getOnlyElement(Dependency.tryCreateFromInstanceofCheck(instanceofCheck));

        Assertions.assertThatDependency(dependency)
                .matches(ClassWithDependencyOnInstanceofCheck.class, InstanceOfCheckTarget.class)
                .hasDescription(memberWithInstanceofCheck.getFullName(), "checks instanceof", InstanceOfCheckTarget.class.getName())
                .inLocation(ClassWithDependencyOnInstanceofCheck.class, expectedLineNumber);
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

        Dependency dependency = getOnlyElement(Dependency.tryCreateFromAnnotation(annotation));
        assertThatType(dependency.getOriginClass()).isEqualTo(annotatedClass);
        assertThatType(dependency.getTargetClass()).matches(annotationClass);
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

        Dependency dependency = getOnlyElement(Dependency.tryCreateFromAnnotation(annotation));
        assertThatType(dependency.getOriginClass()).matches(ClassWithAnnotatedMembers.class);
        assertThatType(dependency.getTargetClass()).matches(annotationClass);
        assertThat(dependency.getDescription()).as("description")
                .contains(annotatedMember.getDescription() + " is annotated with <" + annotationClass.getName() + ">");
    }

    @Test
    @UseDataProvider("annotated_classes")
    public void Dependency_from_class_annotation_member(JavaClass annotatedClass) {
        JavaAnnotation<?> annotation = annotatedClass.getAnnotationOfType(SomeAnnotation.class.getName());
        JavaClass memberType = ((JavaClass) annotation.get("value").get());

        Dependency dependency = getOnlyElement(Dependency.tryCreateFromAnnotationMember(annotation, memberType));
        assertThatType(dependency.getOriginClass()).isEqualTo(annotatedClass);
        assertThatType(dependency.getTargetClass()).isEqualTo(memberType);
        assertThat(dependency.getDescription()).as("description")
                .contains("Class <" + annotatedClass.getName() + "> has annotation member of type <" + memberType.getName() + ">");
    }

    @Test
    @UseDataProvider("annotated_members")
    public void Dependency_from_member_annotation_member(JavaMember annotatedMember) {
        JavaAnnotation<?> annotation = annotatedMember.getAnnotationOfType(SomeAnnotation.class.getName());
        JavaClass memberType = ((JavaClass) annotation.get("value").get());

        Dependency dependency = getOnlyElement(Dependency.tryCreateFromAnnotationMember(annotation, memberType));
        assertThatType(dependency.getOriginClass()).isEqualTo(annotatedMember.getOwner());
        assertThatType(dependency.getTargetClass()).isEqualTo(memberType);
        assertThat(dependency.getDescription()).as("description")
                .contains(annotatedMember.getDescription() + " has annotation member of type <" + memberType.getName() + ">");
    }

    @Test
    public void Dependency_from_class_type_parameter() {
        @SuppressWarnings("unused")
        class ClassWithTypeParameters<T extends String> {
        }

        JavaClass javaClass = importClassesWithContext(ClassWithTypeParameters.class, String.class).get(ClassWithTypeParameters.class);
        JavaTypeVariable<?> typeParameter = javaClass.getTypeParameters().get(0);

        Dependency dependency = getOnlyElement(Dependency.tryCreateFromTypeParameter(typeParameter, typeParameter.getUpperBounds().get(0).toErasure()));

        assertThatType(dependency.getOriginClass()).matches(ClassWithTypeParameters.class);
        assertThatType(dependency.getTargetClass()).matches(String.class);
        assertThat(dependency.getDescription()).as("description").contains(String.format(
                "Class <%s> has type parameter '%s' depending on <%s> in (%s.java:0)",
                ClassWithTypeParameters.class.getName(), typeParameter.getName(), String.class.getName(), getClass().getSimpleName()));
    }

    @Test
    public void Dependency_from_constructor_type_parameter() {
        @SuppressWarnings("unused")
        class ConstructorWithTypeParameters {
            <T extends String> ConstructorWithTypeParameters() {
            }
        }

        JavaClass javaClass = importClassesWithContext(ConstructorWithTypeParameters.class, String.class).get(ConstructorWithTypeParameters.class);
        JavaConstructor origin = javaClass.getConstructor(getClass());
        JavaTypeVariable<?> typeParameter = origin.getTypeParameters().get(0);

        Dependency dependency = getOnlyElement(Dependency.tryCreateFromTypeParameter(typeParameter, typeParameter.getUpperBounds().get(0).toErasure()));

        assertThatType(dependency.getOriginClass()).matches(ConstructorWithTypeParameters.class);
        assertThatType(dependency.getTargetClass()).matches(String.class);
        assertThat(dependency.getDescription()).as("description").contains(String.format(
                "Constructor <%s> has type parameter '%s' depending on <%s> in (%s.java:0)",
                origin.getFullName(), typeParameter.getName(), String.class.getName(), getClass().getSimpleName()));
    }

    @Test
    public void Dependency_from_method_type_parameter() {
        @SuppressWarnings("unused")
        class MethodWithTypeParameters {
            <T extends String> T methodWithTypeParameters() {
                return null;
            }
        }

        JavaClass javaClass = importClassesWithContext(MethodWithTypeParameters.class, String.class).get(MethodWithTypeParameters.class);
        JavaMethod origin = javaClass.getMethod("methodWithTypeParameters");
        JavaTypeVariable<?> typeParameter = origin.getTypeParameters().get(0);

        Dependency dependency = getOnlyElement(Dependency.tryCreateFromTypeParameter(typeParameter, typeParameter.getUpperBounds().get(0).toErasure()));

        assertThatType(dependency.getOriginClass()).matches(MethodWithTypeParameters.class);
        assertThatType(dependency.getTargetClass()).matches(String.class);
        assertThat(dependency.getDescription()).as("description").contains(String.format(
                "Method <%s> has type parameter '%s' depending on <%s> in (%s.java:0)",
                origin.getFullName(), typeParameter.getName(), String.class.getName(), getClass().getSimpleName()));
    }

    @Test
    public void Dependency_from_generic_superclass_type_arguments() {
        @SuppressWarnings("unused")
        class Base<A> {
        }
        @SuppressWarnings("unused")
        class ClassWithGenericSuperclass extends Base<String> {
        }

        JavaClass javaClass = importClassesWithContext(ClassWithGenericSuperclass.class, String.class).get(ClassWithGenericSuperclass.class);
        JavaParameterizedType genericSuperclass = (JavaParameterizedType) javaClass.getSuperclass().get();

        Dependency dependency = getOnlyElement(Dependency.tryCreateFromGenericSuperclassTypeArguments(javaClass, genericSuperclass, (JavaClass) genericSuperclass.getActualTypeArguments().get(0)));

        assertThatType(dependency.getOriginClass()).matches(ClassWithGenericSuperclass.class);
        assertThatType(dependency.getTargetClass()).matches(String.class);
        assertThat(dependency.getDescription()).as("description").contains(String.format(
                "Class <%s> has generic superclass <%s<%s>> with type argument depending on <%s> in (%s.java:0)",
                ClassWithGenericSuperclass.class.getName(),
                Base.class.getName(), String.class.getName(),
                String.class.getName(),
                getClass().getSimpleName()));
    }

    @SuppressWarnings("unused")
    private interface InterfaceWithTypeParameter<T> {
    }

    @Test
    public void Dependency_from_generic_interface_type_arguments() {
        @SuppressWarnings("unused")
        class ClassWithGenericInterface implements InterfaceWithTypeParameter<String> {
        }

        JavaClass javaClass = importClassesWithContext(ClassWithGenericInterface.class, InterfaceWithTypeParameter.class, String.class).get(ClassWithGenericInterface.class);
        JavaParameterizedType genericInterface = (JavaParameterizedType) getOnlyElement(javaClass.getInterfaces());

        Dependency dependency = getOnlyElement(Dependency.tryCreateFromGenericInterfaceTypeArgument(javaClass, genericInterface, (JavaClass) genericInterface.getActualTypeArguments().get(0)));

        assertThatType(dependency.getOriginClass()).matches(ClassWithGenericInterface.class);
        assertThatType(dependency.getTargetClass()).matches(String.class);
        assertThat(dependency.getDescription()).as("description").contains(String.format(
                "Class <%s> has generic interface <%s<%s>> with type argument depending on <%s> in (%s.java:0)",
                ClassWithGenericInterface.class.getName(),
                InterfaceWithTypeParameter.class.getName(), String.class.getName(),
                String.class.getName(),
                getClass().getSimpleName()));
    }

    @Test
    public void Dependency_from_generic_field_type_arguments() {
        @SuppressWarnings("unused")
        class SomeGenericType<T> {
        }
        @SuppressWarnings("unused")
        class SomeClass {
            SomeGenericType<String> field;
        }

        JavaField field = importClassesWithContext(SomeClass.class, SomeGenericType.class, String.class)
                .get(SomeClass.class).getField("field");
        JavaClass typeArgumentDependency = (JavaClass) ((JavaParameterizedType) field.getType()).getActualTypeArguments().get(0);

        Dependency dependency = getOnlyElement(Dependency.tryCreateFromGenericFieldTypeArgument(field, typeArgumentDependency));

        assertThatType(dependency.getOriginClass()).matches(SomeClass.class);
        assertThatType(dependency.getTargetClass()).matches(String.class);
        assertThat(dependency.getDescription()).as("description").contains(String.format(
                "Field <%s> has generic type <%s<%s>> with type argument depending on <%s> in (%s.java:0)",
                field.getFullName(),
                SomeGenericType.class.getName(), String.class.getName(),
                String.class.getName(),
                getClass().getSimpleName()));
    }

    @Test
    public void Dependency_from_referenced_class_object() {
        JavaMethod origin = new ClassFileImporter()
                .importClass(DependenciesOnClassObjects.class)
                .getMethod("referencedClassObjectsInMethod");
        ReferencedClassObject referencedClassObject = getOnlyElement(FluentIterable.from(origin.getReferencedClassObjects())
                .filter(toGuava(rawType(FileSystem.class)))
                .toSet());

        Dependency dependency = getOnlyElement(Dependency.tryCreateFromReferencedClassObject(referencedClassObject));

        assertThatType(dependency.getOriginClass()).matches(DependenciesOnClassObjects.class);
        assertThatType(dependency.getTargetClass()).matches(FileSystem.class);
        assertThat(dependency.getDescription()).as("description")
                .contains(String.format("Method <%s> references class object <%s> in (%s.java:%d)",
                        origin.getFullName(), FileSystem.class.getName(), DependenciesOnClassObjects.class.getSimpleName(), 22));
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
        assertThatType(GET_ORIGIN_CLASS.apply(createDependency(Origin.class, Target.class))).matches(Origin.class);
        assertThatType(GET_TARGET_CLASS.apply(createDependency(Origin.class, Target.class))).matches(Target.class);
    }

    private Dependency createDependency(JavaClass origin, JavaClass target) {
        Dependency dependency = Dependency.fromInheritance(origin, target);
        assertThatType(dependency.getOriginClass()).as("origin class").isEqualTo(origin);
        assertThatType(dependency.getTargetClass()).as("target class").isEqualTo(target);
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

    private interface DependencySubinterface extends DependencyInterface {
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
    private interface InterfaceWithDependencyOnAnnotation {
    }

    @SomeAnnotation(SomeMemberType.class)
    private static class ClassWithDependencyOnAnnotation {
    }

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
