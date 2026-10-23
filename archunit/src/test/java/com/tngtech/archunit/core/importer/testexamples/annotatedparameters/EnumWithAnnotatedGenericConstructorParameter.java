package com.tngtech.archunit.core.importer.testexamples.annotatedparameters;

import java.util.Collections;
import java.util.List;

import com.tngtech.archunit.core.importer.testexamples.annotatedparameters.ClassWithMethodWithAnnotatedParameters.SimpleParameterAnnotation;

@SuppressWarnings("unused")
public enum EnumWithAnnotatedGenericConstructorParameter {
    INSTANCE(new Object(), Collections.emptyList());

    EnumWithAnnotatedGenericConstructorParameter(Object notAnnotated, @SimpleParameterAnnotation("test") List<String> annotated) {
    }
}
