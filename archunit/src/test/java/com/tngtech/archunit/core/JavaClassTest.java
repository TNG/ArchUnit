package com.tngtech.archunit.core;

import java.io.Serializable;
import java.lang.annotation.Retention;

import org.junit.Test;

import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.JavaStaticInitializer.STATIC_INITIALIZER_NAME;
import static com.tngtech.archunit.core.TestUtils.javaClass;
import static com.tngtech.archunit.core.TestUtils.simulateCallFrom;
import static com.tngtech.archunit.testutil.Conditions.codeUnitWithSignature;
import static com.tngtech.archunit.testutil.Conditions.containing;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaClassTest {

    @Test
    public void finds_fields_and_methods() {
        JavaClass javaClass = new JavaClass.Builder().withType(ClassWithTwoFieldsAndTwoMethods.class).build();

        assertThat(javaClass.reflect()).isEqualTo(ClassWithTwoFieldsAndTwoMethods.class);
        assertThat(javaClass.getFields()).hasSize(2);
        assertThat(javaClass.getMethods()).hasSize(2);

        for (JavaField field : javaClass.getFields()) {
            assertThat(field.getOwner()).isSameAs(javaClass);
        }
        for (JavaCodeUnit<?, ?> method : javaClass.getCodeUnits()) {
            assertThat(method.getOwner()).isSameAs(javaClass);
        }
    }

    @Test
    public void finds_constructors() {
        JavaClass javaClass = new JavaClass.Builder().withType(ClassWithSeveralConstructors.class).build();

        assertThat(javaClass.getConstructors()).hasSize(3);
        assertThat(javaClass.getConstructors()).is(containing(codeUnitWithSignature(CONSTRUCTOR_NAME)));
        assertThat(javaClass.getConstructors()).is(containing(codeUnitWithSignature(CONSTRUCTOR_NAME, String.class)));
        assertThat(javaClass.getConstructors()).is(containing(codeUnitWithSignature(CONSTRUCTOR_NAME, int.class, Object[].class)));
    }

    @Test
    public void finds_static_Initializer() {
        JavaClass javaClass = new JavaClass.Builder().withType(Object.class).build();

        assertThat(javaClass.getStaticInitializer()).isNotNull();
        assertThat(javaClass.getStaticInitializer().getName()).isEqualTo(STATIC_INITIALIZER_NAME);
    }

    @Test
    public void equals_works() {
        JavaClass javaClass = new JavaClass.Builder().withType(ClassWithTwoFieldsAndTwoMethods.class).build();
        JavaClass equalClass = new JavaClass.Builder().withType(ClassWithTwoFieldsAndTwoMethods.class).build();
        JavaClass differentClass = new JavaClass.Builder().withType(SuperClassWithFieldAndMethod.class).build();

        assertThat(javaClass).isEqualTo(javaClass);
        assertThat(javaClass).isEqualTo(equalClass);
        assertThat(javaClass).isNotEqualTo(differentClass);
    }

    @Test
    public void anonymous_class_has_package_of_declaring_class() {
        JavaClass anonymous = new JavaClass.Builder()
                .withType(new Serializable() {
                }.getClass())
                .build();

        assertThat(anonymous.getPackage()).isEqualTo(getClass().getPackage().getName());
    }

    @Test
    public void inner_class_has_package_of_declaring_class() {
        JavaClass anonymous = new JavaClass.Builder()
                .withType(ClassWithInnerClass.Inner.class)
                .build();

        assertThat(anonymous.getPackage()).isEqualTo(getClass().getPackage().getName());
    }

    @Test
    public void Array_class_has_default_package() {
        JavaClass arrayType = new JavaClass.Builder().withType(JavaClassTest[].class).build();

        assertThat(arrayType.getPackage()).isEmpty();
    }

    @Test
    public void superclasses_are_found() {
        JavaClass clazz = javaClass(ClassWithTwoFieldsAndTwoMethods.class);

        assertThat(clazz.getAllSuperClasses()).containsExactly(
                javaClass(SuperClassWithFieldAndMethod.class), javaClass(Parent.class), javaClass(Object.class));
    }

    @Test
    public void hierarchy_is_found() {
        JavaClass clazz = javaClass(ClassWithTwoFieldsAndTwoMethods.class);

        assertThat(clazz.getClassHierarchy()).containsExactly(
                clazz, javaClass(SuperClassWithFieldAndMethod.class), javaClass(Parent.class), javaClass(Object.class));
    }

    @Test
    public void Annotations_are_reported() {
        assertThat(javaClass(Parent.class).isAnnotationPresent(SomeAnnotation.class))
                .as("Parent is annotated with @" + SomeAnnotation.class.getSimpleName()).isTrue();
        assertThat(javaClass(Parent.class).isAnnotationPresent(Retention.class))
                .as("Parent is annotated with @" + Retention.class.getSimpleName()).isFalse();
    }

    @Test
    public void allAccesses_contains_accesses_from_superclass() {
        JavaClass javaClass = javaClass(ClassWithTwoFieldsAndTwoMethods.class);
        JavaClass anotherClass = javaClass(Object.class);
        simulateCallFrom(javaClass.getMethod("stringMethod"), 8).to(anotherClass.getMethod("toString"));
        simulateCallFrom(javaClass.getSuperClass().get().getMethod("objectMethod"), 8).to(anotherClass.getMethod("toString"));

        assertThat(javaClass.getAccessesFromSelf()).extractingResultOf("getOriginClass").containsOnly(javaClass);
        assertThat(javaClass.getAllAccessesFromSelf()).extractingResultOf("getOriginClass")
                .containsOnly(javaClass, javaClass.getSuperClass().get());
    }

    static class ClassWithTwoFieldsAndTwoMethods extends SuperClassWithFieldAndMethod {
        String stringField;
        private int intField;

        void voidMethod() {
        }

        protected String stringMethod() {
            return null;
        }
    }

    static abstract class SuperClassWithFieldAndMethod extends Parent {
        private Object objectField;

        private Object objectMethod() {
            return null;
        }
    }

    @SomeAnnotation
    static abstract class Parent {
    }

    static class ClassWithSeveralConstructors {
        private ClassWithSeveralConstructors() {
        }

        ClassWithSeveralConstructors(String string) {
        }

        public ClassWithSeveralConstructors(int number, Object[] objects) {
        }
    }

    static class ClassWithInnerClass {
        class Inner {
        }
    }

    @Retention(RUNTIME)
    @interface SomeAnnotation {
    }
}