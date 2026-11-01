package com.tngtech.archunit.core.importer;

import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.core.importer.ConstructorParametersAssertion.assertThatParametersOf;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteClass.concreteClass;

/**
 * Since Java 25 a constructor may contain statements before the explicit {@code super(...)} call (JEP 513).
 * Local and anonymous classes declared in this prologue cannot refer to the instance under construction,
 * so {@code javac} gives them the enclosing instance of the <em>enclosing</em> class instead. I.e. the enclosing instance
 * parameter of their constructors is not of the immediately enclosing class, in contrast to classes declared after the
 * {@code super(...)} call (compare #1772 and {@link ClassFileImporterConstructorParametersTest}).
 */
public class ClassFileImporterConstructorParametersFlexibleConstructorBodiesTest {

    @Test
    void constructor_of_local_class_declared_before_super_call_has_enclosing_instance_of_enclosing_class() {
        Outer.Inner inner = new Outer().new Inner("param");

        // the descriptor is (Outer, String, String): the enclosing instance is Outer, not Inner, followed by the declared parameter
        // and the captured constructor parameter; javac writes a Signature attribute (String) because of the captured variable
        assertThatParametersOf(inner.localClassDeclaredBeforeSuperCall)
                .haveRawTypes(Outer.class, String.class, String.class)
                .haveTypes(concreteClass(String.class))
                .haveCount(1)
                .haveFewerParametersThanReflection(2);
    }

    @Test
    void constructor_of_anonymous_class_declared_before_super_call_has_enclosing_instance_of_enclosing_class() {
        Outer.Inner inner = new Outer().new Inner("param");

        assertThatParametersOf(inner.anonymousClassDeclaredBeforeSuperCall)
                .haveRawTypes(Outer.class, String.class)
                .haveTypes(concreteClass(Outer.class), concreteClass(String.class))
                .haveCount(2)
                .haveSameCountAsReflection();
    }

    @Test
    void constructor_of_anonymous_class_declared_after_super_call_has_enclosing_instance_of_immediately_enclosing_class() {
        Outer.Inner inner = new Outer().new Inner("param");

        assertThatParametersOf(inner.anonymousClassDeclaredAfterSuperCall)
                .haveRawTypes(Outer.Inner.class, String.class)
                .haveTypes(concreteClass(Outer.Inner.class), concreteClass(String.class))
                .haveCount(2)
                .haveSameCountAsReflection();
    }

    @SuppressWarnings({"unused", "InnerClassMayBeStatic"})
    static class Outer {
        class Inner {
            final Class<?> localClassDeclaredBeforeSuperCall;
            final Class<?> anonymousClassDeclaredBeforeSuperCall;
            final Class<?> anonymousClassDeclaredAfterSuperCall;

            Inner(String param) {
                class LocalClassDeclaredBeforeSuperCall {
                    LocalClassDeclaredBeforeSuperCall(String otherParam) {
                    }

                    int length() {
                        return param.length();
                    }
                }
                Class<?> localClass = LocalClassDeclaredBeforeSuperCall.class;
                Class<?> anonymousClass = new Object() {
                    final int length = param.length();
                }.getClass();
                super();
                localClassDeclaredBeforeSuperCall = localClass;
                anonymousClassDeclaredBeforeSuperCall = anonymousClass;
                anonymousClassDeclaredAfterSuperCall = new Object() {
                    final int length = param.length();
                }.getClass();
            }
        }
    }
}
