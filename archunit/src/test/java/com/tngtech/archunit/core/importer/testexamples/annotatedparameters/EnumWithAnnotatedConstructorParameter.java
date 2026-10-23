package com.tngtech.archunit.core.importer.testexamples.annotatedparameters;

import com.tngtech.archunit.core.importer.testexamples.annotatedparameters.ClassWithMethodWithAnnotatedParameters.SimpleParameterAnnotation;

@SuppressWarnings("unused")
public enum EnumWithAnnotatedConstructorParameter {
    INSTANCE(new Object(), "");

    EnumWithAnnotatedConstructorParameter(Object notAnnotated, @SimpleParameterAnnotation("test") String annotated) {
    }
}
