package com.tngtech.archunit.core;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.DescribedPredicate;
import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.Test;

import static com.tngtech.archunit.core.JavaClass.Predicates.INTERFACES;
import static com.tngtech.archunit.core.JavaClass.Predicates.assignableFrom;
import static com.tngtech.archunit.core.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.JavaClass.Predicates.resideInPackage;
import static com.tngtech.archunit.core.JavaClass.Predicates.withType;
import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.TestUtils.javaClassViaReflection;
import static com.tngtech.archunit.core.TestUtils.javaClassesViaReflection;
import static com.tngtech.archunit.core.TestUtils.simulateCall;
import static com.tngtech.archunit.testutil.Conditions.codeUnitWithSignature;
import static com.tngtech.archunit.testutil.Conditions.containing;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JavaClassTest {

    @Test
    public void finds_fields_and_methods() {
        JavaClass javaClass = javaClassViaReflection(ClassWithTwoFieldsAndTwoMethods.class);

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
        JavaClass javaClass = javaClassViaReflection(ClassWithSeveralConstructors.class);

        assertThat(javaClass.getConstructors()).hasSize(3);
        assertThat(javaClass.getConstructors()).is(containing(codeUnitWithSignature(CONSTRUCTOR_NAME)));
        assertThat(javaClass.getConstructors()).is(containing(codeUnitWithSignature(CONSTRUCTOR_NAME, String.class)));
        assertThat(javaClass.getConstructors()).is(containing(codeUnitWithSignature(CONSTRUCTOR_NAME, int.class, Object[].class)));
    }

    @Test
    public void anonymous_class_has_package_of_declaring_class() {
        JavaClass anonymous = javaClassViaReflection(new Serializable() {}.getClass());

        assertThat(anonymous.getPackage()).isEqualTo(getClass().getPackage().getName());
    }

    @Test
    public void inner_class_has_package_of_declaring_class() {
        JavaClass anonymous = javaClassViaReflection(ClassWithInnerClass.Inner.class);

        assertThat(anonymous.getPackage()).isEqualTo(getClass().getPackage().getName());
    }

    @Test
    public void Array_class_has_default_package() {
        JavaClass arrayType = javaClassViaReflection(JavaClassTest[].class);

        assertThat(arrayType.getPackage()).isEmpty();
    }

    @Test
    public void superclasses_are_found() {
        JavaClass clazz = javaClassesViaReflection(ClassWithTwoFieldsAndTwoMethods.class, SuperClassWithFieldAndMethod.class, Parent.class)
                .get(ClassWithTwoFieldsAndTwoMethods.class);

        assertThat(clazz.getAllSuperClasses()).extracting("name").containsExactly(
                SuperClassWithFieldAndMethod.class.getName(),
                Parent.class.getName(),
                Object.class.getName());
    }

    @Test
    public void hierarchy_is_found() {
        JavaClass clazz = javaClassesViaReflection(ClassWithTwoFieldsAndTwoMethods.class, SuperClassWithFieldAndMethod.class, Parent.class)
                .get(ClassWithTwoFieldsAndTwoMethods.class);

        assertThat(clazz.getClassHierarchy()).extracting("name").containsExactly(
                clazz.getName(),
                SuperClassWithFieldAndMethod.class.getName(),
                Parent.class.getName(),
                Object.class.getName());
    }

    @Test
    public void Annotations_are_reported() {
        assertThat(javaClassViaReflection(Parent.class).isAnnotatedWith(SomeAnnotation.class))
                .as("Parent is annotated with @" + SomeAnnotation.class.getSimpleName()).isTrue();
        assertThat(javaClassViaReflection(Parent.class).isAnnotatedWith(Retention.class))
                .as("Parent is annotated with @" + Retention.class.getSimpleName()).isFalse();
    }

    @Test
    public void allAccesses_contains_accesses_from_superclass() {
        JavaClass javaClass = javaClassesViaReflection(ClassWithTwoFieldsAndTwoMethods.class, SuperClassWithFieldAndMethod.class, Parent.class)
                .get(ClassWithTwoFieldsAndTwoMethods.class);
        JavaClass anotherClass = javaClassViaReflection(Object.class);
        simulateCall().from(javaClass.getMethod("stringMethod"), 8).to(anotherClass.getMethod("toString"));
        simulateCall().from(javaClass.getSuperClass().get().getMethod("objectMethod"), 8).to(anotherClass.getMethod("toString"));

        assertThat(javaClass.getAccessesFromSelf()).extractingResultOf("getOriginOwner").containsOnly(javaClass);
        assertThat(javaClass.getAllAccessesFromSelf()).extractingResultOf("getOriginOwner")
                .containsOnly(javaClass, javaClass.getSuperClass().get());
    }

    @Test
    public void predicate_withType() {
        assertThat(withType(Parent.class).apply(javaClassViaReflection(Parent.class)))
                .as("withType(Parent) matches JavaClass Parent").isTrue();
        assertThat(withType(Parent.class).apply(javaClassViaReflection(SuperClassWithFieldAndMethod.class)))
                .as("withType(Parent) matches JavaClass SuperClassWithFieldAndMethod").isFalse();

        assertThat(withType(System.class).getDescription()).isEqualTo("with type java.lang.System");
    }

    @Test
    public void predicate_assignableFrom() {
        assertThatAssignable().from(SuperClassWithFieldAndMethod.class)
                .to(SuperClassWithFieldAndMethod.class)
                .isTrue();
        assertThatAssignable().from(ClassWithTwoFieldsAndTwoMethods.class)
                .to(SuperClassWithFieldAndMethod.class)
                .isTrue();
        assertThatAssignable().from(SuperClassWithFieldAndMethod.class)
                .to(InterfaceWithMethod.class)
                .isTrue();
        assertThatAssignable().from(ClassWithTwoFieldsAndTwoMethods.class)
                .via(SuperClassWithFieldAndMethod.class)
                .to(InterfaceWithMethod.class)
                .isTrue();
        assertThatAssignable().from(InterfaceWithMethod.class)
                .to(InterfaceWithMethod.class)
                .isTrue();
        assertThatAssignable().from(Parent.class)
                .to(InterfaceWithMethod.class)
                .isFalse();
        assertThatAssignable().from(SuperClassWithFieldAndMethod.class)
                .to(Parent.class)
                .isTrue();
        assertThatAssignable().from(SuperClassWithFieldAndMethod.class)
                .to(ClassWithTwoFieldsAndTwoMethods.class)
                .isFalse();
        assertThatAssignable().from(Parent.class)
                .to(SuperClassWithFieldAndMethod.class)
                .isFalse();

        assertThat(assignableFrom(System.class).getDescription()).isEqualTo("assignable from java.lang.System");
    }

    @Test
    public void predicate_assignableTo() {
        assertThatAssignable().to(SuperClassWithFieldAndMethod.class)
                .from(SuperClassWithFieldAndMethod.class)
                .isTrue();
        assertThatAssignable().to(ClassWithTwoFieldsAndTwoMethods.class)
                .from(SuperClassWithFieldAndMethod.class)
                .isFalse();
        assertThatAssignable().to(InterfaceWithMethod.class)
                .from(InterfaceWithMethod.class)
                .isTrue();
        assertThatAssignable().to(InterfaceWithMethod.class)
                .from(SuperClassWithFieldAndMethod.class)
                .isTrue();
        assertThatAssignable().to(InterfaceWithMethod.class)
                .via(SuperClassWithFieldAndMethod.class)
                .from(ClassWithTwoFieldsAndTwoMethods.class)
                .isTrue();
        assertThatAssignable().to(InterfaceWithMethod.class)
                .from(Parent.class)
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

        assertThat(assignableTo(System.class).getDescription()).isEqualTo("assignable to java.lang.System");
    }

    @Test
    public void predicate_interfaces() {
        assertThat(INTERFACES.apply(javaClassViaReflection(Serializable.class))).as("Predicate matches").isTrue();
        assertThat(INTERFACES.apply(javaClassViaReflection(Object.class))).as("Predicate matches").isFalse();
        assertThat(INTERFACES.getDescription()).isEqualTo("interfaces");
    }

    @Test
    public void predicate_reside_in_package() {
        JavaClass clazz = fakeClassWithPackage("some.arbitrary.pkg");

        assertThat(resideInPackage("some..pkg").apply(clazz)).as("package matches").isTrue();

        clazz = fakeClassWithPackage("wrong.arbitrary.pkg");

        assertThat(resideInPackage("some..pkg").apply(clazz)).as("package matches").isFalse();

        assertThat(resideInPackage("..any..").getDescription())
                .isEqualTo("reside in package '..any..'");
    }

    @Test
    public void predicate_reside_in_any_package() {
        JavaClass clazz = fakeClassWithPackage("some.arbitrary.pkg");

        assertThat(resideInAnyPackage("any.thing", "some..pkg").apply(clazz)).as("package matches").isTrue();

        clazz = fakeClassWithPackage("wrong.arbitrary.pkg");

        assertThat(resideInAnyPackage("any.thing", "some..pkg").apply(clazz)).as("package matches").isFalse();

        assertThat(resideInAnyPackage("any.thing", "..any..").getDescription())
                .isEqualTo("reside in any package 'any.thing', '..any..'");
    }

    private JavaClass fakeClassWithPackage(String pkg) {
        JavaClass javaClass = mock(JavaClass.class);
        when(javaClass.getPackage()).thenReturn(pkg);
        return javaClass;
    }

    private static AssignableAssert assertThatAssignable() {
        return new AssignableAssert();
    }

    private static class AssignableAssert {
        private String message;
        private DescribedPredicate<JavaClass> assignable;
        private Class<?> firstType;

        public FromEvaluation from(Class<?> type) {
            firstType = type;
            message = String.format("assignableFrom(%s) matches ", type.getSimpleName());
            assignable = assignableFrom(type);
            return new FromEvaluation();
        }

        public ToEvaluation to(Class<?> type) {
            firstType = type;
            message = String.format("assignableTo(%s) matches ", type.getSimpleName());
            assignable = assignableTo(type);
            return new ToEvaluation();
        }

        private class FromEvaluation extends Evaluation<FromEvaluation> {
            public Evaluation to(Class<?> toType) {
                return evaluationToType(toType);
            }
        }

        private class ToEvaluation extends Evaluation<ToEvaluation> {
            public Evaluation from(Class<?> fromType) {
                return evaluationToType(fromType);
            }
        }

        private class Evaluation<SELF> {
            private AbstractBooleanAssert<?> assignableAssertion;

            private final Set<Class<?>> additionalTypes = new HashSet<>();

            // NOTE: We need all the classes in the context to create realistic hierarchies
            @SuppressWarnings("unchecked")
            SELF via(Class<?> type) {
                additionalTypes.add(type);
                return (SELF) this;
            }

            Evaluation evaluationToType(Class<?> secondType) {
                Class<?>[] types = ImmutableSet.<Class<?>>builder()
                        .addAll(additionalTypes).add(firstType).add(secondType)
                        .build().toArray(new Class<?>[0]);
                JavaClass javaClass = javaClassesViaReflection(types).get(secondType);
                assignableAssertion = assertThat(assignable.apply(javaClass))
                        .as(message + secondType.getSimpleName());
                return this;
            }

            public void isTrue() {
                assignableAssertion.isTrue();
            }

            public void isFalse() {
                assignableAssertion.isFalse();
            }
        }
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

    static abstract class SuperClassWithFieldAndMethod extends Parent implements InterfaceWithMethod {
        private Object objectField;

        @Override
        public Object objectMethod() {
            return null;
        }
    }

    interface InterfaceWithMethod {
        Object objectMethod();
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