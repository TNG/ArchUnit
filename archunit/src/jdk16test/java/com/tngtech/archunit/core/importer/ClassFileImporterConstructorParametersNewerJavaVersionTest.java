package com.tngtech.archunit.core.importer;

import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.core.importer.ConstructorParametersAssertion.assertThatParametersOf;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteClass.concreteClass;

/**
 * Since Java 16 inner classes may declare static members, which opens static contexts inside of inner classes.
 * Classes declared in such a context have no enclosing instance, even though the lexically enclosing class is an inner class
 * (compare #1772 and {@link ClassFileImporterConstructorParametersTest}).
 */
public class ClassFileImporterConstructorParametersNewerJavaVersionTest {

    @Test
    void constructor_of_static_nested_class_of_inner_class_has_no_enclosing_instance() {
        assertThatParametersOf(Outer.Inner.StaticNestedClassOfInnerClass.class)
                .haveRawTypes(String.class)
                .haveTypes(concreteClass(String.class))
                .haveCount(1)
                .haveSameCountAsReflection();
    }

    @Test
    void constructor_of_inner_class_of_static_nested_class_of_inner_class_has_the_static_nested_class_as_enclosing_instance() {
        assertThatParametersOf(Outer.Inner.StaticNestedClassOfInnerClass.InnerClassOfStaticNestedClass.class)
                .haveRawTypes(Outer.Inner.StaticNestedClassOfInnerClass.class, String.class)
                .haveTypes(concreteClass(Outer.Inner.StaticNestedClassOfInnerClass.class), concreteClass(String.class))
                .haveCount(2)
                .haveSameCountAsReflection();
    }

    @Test
    void constructor_of_local_class_in_static_method_of_inner_class_has_no_enclosing_instance() {
        assertThatParametersOf(Outer.Inner.localClassInStaticMethod())
                .haveRawTypes(String.class)
                .haveTypes(concreteClass(String.class))
                .haveCount(1)
                .haveSameCountAsReflection();
    }

    @Test
    void constructor_of_record_nested_in_inner_class_has_no_enclosing_instance() {
        assertThatParametersOf(Outer.Inner.RecordInInnerClass.class)
                .haveRawTypes(String.class)
                .haveTypes(concreteClass(String.class))
                .haveCount(1)
                .haveSameCountAsReflection();
    }

    @SuppressWarnings({"unused", "InnerClassMayBeStatic"})
    static class Outer {
        class Inner {
            static class StaticNestedClassOfInnerClass {
                StaticNestedClassOfInnerClass(String param) {
                }

                class InnerClassOfStaticNestedClass {
                    InnerClassOfStaticNestedClass(String param) {
                    }
                }
            }

            record RecordInInnerClass(String component) {
            }

            static Class<?> localClassInStaticMethod() {
                class LocalClassInStaticMethodOfInnerClass {
                    LocalClassInStaticMethodOfInnerClass(String param) {
                    }
                }
                return LocalClassInStaticMethodOfInnerClass.class;
            }
        }
    }
}
