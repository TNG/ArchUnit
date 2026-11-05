package com.tngtech.archunit.core.importer.testexamples.compileraddedparameters;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Anonymous classes in the contexts that matter for the parameters of their constructors: whether there is an enclosing
 * instance, whether arguments are forwarded to the superclass constructor and whether local variables are captured.
 */
@SuppressWarnings("unused")
public class AnonymousClassExamples {
    public Class<?> anonymousClassInInstanceMethod() {
        return new Object() {
        }.getClass();
    }

    public Class<?> anonymousClassInInstanceMethodForwardingSuperclassConstructorArgument() {
        return new ClassWithConstructorParameter("param") {
        }.getClass();
    }

    public Class<?> anonymousClassInInstanceMethodCapturingVariable() {
        int captured = 42;
        return new ClassWithConstructorParameter("param") {
            final int value = captured;
        }.getClass();
    }

    public static Class<?> anonymousClassInStaticMethodCapturingVariable() {
        int captured = 42;
        return new ClassWithConstructorParameter("param") {
            final int value = captured;
        }.getClass();
    }

    public static Class<?> anonymousClassInStaticMethodCapturingGenericVariable() {
        List<String> captured = emptyList();
        return new ClassWithConstructorParameter("param") {
            final List<String> value = captured;
        }.getClass();
    }

    /**
     * @return an instance of an anonymous class whose {@link ClassWithConstructorParameter#nestedAnonymousClass()} returns
     * a second anonymous class declared inside the first one; the two classes capture different local variables
     */
    public static ClassWithConstructorParameter anonymousClassWithNestedAnonymousClassInStaticMethod(String param) {
        int capturedByOuterAnonymousClass = 42;
        double capturedByNestedAnonymousClass = 42.0;
        return new ClassWithConstructorParameter(param) {
            @Override
            public Class<?> nestedAnonymousClass() {
                int local = capturedByOuterAnonymousClass;
                return new ClassWithConstructorParameter(param) {
                    final double value = capturedByNestedAnonymousClass;
                }.getClass();
            }
        };
    }
}
