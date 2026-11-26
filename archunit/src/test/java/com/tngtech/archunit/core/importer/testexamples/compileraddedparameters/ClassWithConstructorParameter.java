package com.tngtech.archunit.core.importer.testexamples.compileraddedparameters;

/**
 * Superclass for anonymous classes that have to forward an argument to the superclass constructor.
 */
@SuppressWarnings("unused")
public class ClassWithConstructorParameter {
    public ClassWithConstructorParameter(String param) {
    }

    public Class<?> nestedAnonymousClass() {
        throw new UnsupportedOperationException("only implemented by anonymous subclasses that declare a nested anonymous class");
    }
}
