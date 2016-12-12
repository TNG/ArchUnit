package com.tngtech.archunit.core;

import java.io.Serializable;
import java.lang.annotation.Retention;

import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.Test;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE;
import static com.tngtech.archunit.core.JavaClass.INTERFACES;
import static com.tngtech.archunit.core.JavaClass.REFLECT;
import static com.tngtech.archunit.core.JavaClass.assignableFrom;
import static com.tngtech.archunit.core.JavaClass.assignableTo;
import static com.tngtech.archunit.core.JavaClass.reflectionAssignableFrom;
import static com.tngtech.archunit.core.JavaClass.reflectionAssignableTo;
import static com.tngtech.archunit.core.JavaClass.withType;
import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.JavaStaticInitializer.STATIC_INITIALIZER_NAME;
import static com.tngtech.archunit.core.TestUtils.importSingle;
import static com.tngtech.archunit.core.TestUtils.javaClass;
import static com.tngtech.archunit.core.TestUtils.simulateCall;
import static com.tngtech.archunit.testutil.Conditions.codeUnitWithSignature;
import static com.tngtech.archunit.testutil.Conditions.containing;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaClassTest {

    @Test
    public void finds_fields_and_methods() {
        JavaClass javaClass = importSingle(ClassWithTwoFieldsAndTwoMethods.class);

        assertThat(javaClass.reflect()).isEqualTo(ClassWithTwoFieldsAndTwoMethods.class);
        assertThat(javaClass.getFields()).hasSize(2);
        assertThat(javaClass.getMethods()).hasSize(2);

        for (JavaField field : javaClass.getFields()) {
            assertThat(field.getOwner()).isSameAs(javaClass);
        }
        for (JavaCodeUnit method : javaClass.getCodeUnits()) {
            assertThat(method.getOwner()).isSameAs(javaClass);
        }
    }

    @Test
    public void finds_constructors() {
        JavaClass javaClass = new JavaClass.Builder().withType(TypeDetails.of(ClassWithSeveralConstructors.class)).build();

        assertThat(javaClass.getConstructors()).hasSize(3);
        assertThat(javaClass.getConstructors()).is(containing(codeUnitWithSignature(CONSTRUCTOR_NAME)));
        assertThat(javaClass.getConstructors()).is(containing(codeUnitWithSignature(CONSTRUCTOR_NAME, String.class)));
        assertThat(javaClass.getConstructors()).is(containing(codeUnitWithSignature(CONSTRUCTOR_NAME, int.class, Object[].class)));
    }

    @Test
    public void finds_static_Initializer() {
        JavaClass javaClass = new JavaClass.Builder().withType(TypeDetails.of(Object.class)).build();

        assertThat(javaClass.getStaticInitializer()).isNotNull();
        assertThat(javaClass.getStaticInitializer().getName()).isEqualTo(STATIC_INITIALIZER_NAME);
    }

    @Test
    public void anonymous_class_has_package_of_declaring_class() {
        JavaClass anonymous = new JavaClass.Builder().withType(TypeDetails.of(new Serializable() {
        }.getClass()))
                .build();

        assertThat(anonymous.getPackage()).isEqualTo(getClass().getPackage().getName());
    }

    @Test
    public void inner_class_has_package_of_declaring_class() {
        JavaClass anonymous = new JavaClass.Builder().withType(TypeDetails.of(ClassWithInnerClass.Inner.class))
                .build();

        assertThat(anonymous.getPackage()).isEqualTo(getClass().getPackage().getName());
    }

    @Test
    public void Array_class_has_default_package() {
        JavaClass arrayType = new JavaClass.Builder().withType(TypeDetails.of(JavaClassTest[].class)).build();

        assertThat(arrayType.getPackage()).isEmpty();
    }

    @Test
    public void superclasses_are_found() {
        JavaClass clazz = javaClass(ClassWithTwoFieldsAndTwoMethods.class);

        assertThat(clazz.getAllSuperClasses()).extracting("name").containsExactly(
                SuperClassWithFieldAndMethod.class.getName(),
                Parent.class.getName(),
                Object.class.getName());
    }

    @Test
    public void hierarchy_is_found() {
        JavaClass clazz = javaClass(ClassWithTwoFieldsAndTwoMethods.class);

        assertThat(clazz.getClassHierarchy()).extracting("name").containsExactly(
                clazz.getName(),
                SuperClassWithFieldAndMethod.class.getName(),
                Parent.class.getName(),
                Object.class.getName());
    }

    @Test
    public void Annotations_are_reported() {
        assertThat(javaClass(Parent.class).isAnnotatedWith(SomeAnnotation.class))
                .as("Parent is annotated with @" + SomeAnnotation.class.getSimpleName()).isTrue();
        assertThat(javaClass(Parent.class).isAnnotatedWith(Retention.class))
                .as("Parent is annotated with @" + Retention.class.getSimpleName()).isFalse();
    }

    @Test
    public void allAccesses_contains_accesses_from_superclass() {
        JavaClass javaClass = javaClass(ClassWithTwoFieldsAndTwoMethods.class);
        JavaClass anotherClass = javaClass(Object.class);
        simulateCall().from(javaClass.getMethod("stringMethod"), 8).to(anotherClass.getMethod("toString"));
        simulateCall().from(javaClass.getSuperClass().get().getMethod("objectMethod"), 8).to(anotherClass.getMethod("toString"));

        assertThat(javaClass.getAccessesFromSelf()).extractingResultOf("getOriginOwner").containsOnly(javaClass);
        assertThat(javaClass.getAllAccessesFromSelf()).extractingResultOf("getOriginOwner")
                .containsOnly(javaClass, javaClass.getSuperClass().get());
    }

    @Test
    public void withType_works() {
        assertThat(withType(Parent.class).apply(javaClass(Parent.class)))
                .as("withType(Parent) matches JavaClass Parent").isTrue();
        assertThat(withType(Parent.class).apply(javaClass(SuperClassWithFieldAndMethod.class)))
                .as("withType(Parent) matches JavaClass SuperClassWithFieldAndMethod").isFalse();
    }

    @Test
    public void assignableFrom_works() {
        assertThatAssignable().from(SuperClassWithFieldAndMethod.class)
                .to(SuperClassWithFieldAndMethod.class)
                .isTrue();
        assertThatAssignable().from(ClassWithTwoFieldsAndTwoMethods.class)
                .to(SuperClassWithFieldAndMethod.class)
                .isTrue();
        assertThatAssignable().from(SuperClassWithFieldAndMethod.class)
                .to(Parent.class)
                .isTrue();
        assertThatAssignable().from(SuperClassWithFieldAndMethod.class)
                .to(ClassWithTwoFieldsAndTwoMethods.class)
                .isFalse();
        assertThatAssignable().from(Parent.class)
                .to(SuperClassWithFieldAndMethod.class)
                .isFalse();
    }

    @Test
    public void assignableTo_works() {
        assertThatAssignable().to(SuperClassWithFieldAndMethod.class)
                .from(SuperClassWithFieldAndMethod.class)
                .isTrue();
        assertThatAssignable().to(ClassWithTwoFieldsAndTwoMethods.class)
                .from(SuperClassWithFieldAndMethod.class)
                .isFalse();
        assertThatAssignable().to(SuperClassWithFieldAndMethod.class)
                .from(Parent.class)
                .isFalse();
        assertThatAssignable().to(SuperClassWithFieldAndMethod.class)
                .from(ClassWithTwoFieldsAndTwoMethods.class)
                .isTrue();
        assertThatAssignable().to(Parent.class)
                .from(SuperClassWithFieldAndMethod.class)
                .isTrue();
    }

    @Test
    public void descriptions() {
        assertThat(withType(System.class).getDescription()).isEqualTo("with type java.lang.System");
        assertThat(reflectionAssignableTo(System.class).getDescription()).isEqualTo("assignable to java.lang.System");
        assertThat(reflectionAssignableFrom(System.class).getDescription()).isEqualTo("assignable from java.lang.System");
        assertThat(assignableTo(System.class).getDescription()).isEqualTo("assignable to java.lang.System");
        assertThat(assignableFrom(System.class).getDescription()).isEqualTo("assignable from java.lang.System");
        assertThat(INTERFACES.getDescription()).isEqualTo("interfaces");
    }

    private static AssignableAssert assertThatAssignable() {
        return new AssignableAssert();
    }

    private static class AssignableAssert {
        private String message;
        private DescribedPredicate<Class<?>> reflectionAssignable;
        private DescribedPredicate<JavaClass> assignable;

        public FromEvaluation from(Class<?> type) {
            message = String.format("assignableFrom(%s) matches ", type.getSimpleName());
            reflectionAssignable = reflectionAssignableFrom(type);
            assignable = assignableFrom(type);
            return new FromEvaluation();
        }

        public ToEvaluation to(Class<?> type) {
            message = String.format("assignableTo(%s) matches ", type.getSimpleName());
            reflectionAssignable = reflectionAssignableTo(type);
            assignable = assignableTo(type);
            return new ToEvaluation();
        }

        private class FromEvaluation extends Evaluation {
            public Evaluation to(Class<?> toType) {
                return evaluationToType(toType);
            }
        }

        private class ToEvaluation extends Evaluation {
            public Evaluation from(Class<?> toType) {
                return evaluationToType(toType);
            }
        }

        private class Evaluation {
            private AbstractBooleanAssert<?> reflectionAssignableAssertion;
            private AbstractBooleanAssert<?> assignableAssertion;

            Evaluation evaluationToType(Class<?> toType) {
                reflectionAssignableAssertion = assertThat(reflectionAssignable.apply(toType))
                        .as(LOWER_UNDERSCORE.to(LOWER_CAMEL, "reflection_" + message) + toType.getSimpleName());
                assignableAssertion = assertThat(assignable.apply(javaClass(toType)))
                        .as(message + toType.getSimpleName());
                return this;
            }

            public void isTrue() {
                reflectionAssignableAssertion.isTrue();
                assignableAssertion.isTrue();
            }

            public void isFalse() {
                reflectionAssignableAssertion.isFalse();
                assignableAssertion.isFalse();
            }
        }
    }

    @Test
    public void REFLECT_works() {
        assertThat(REFLECT.apply(javaClass(Parent.class))).isEqualTo(Parent.class);
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