package com.tngtech.archunit.core.importer.testexamples.annotationfieldimport;

import com.tngtech.archunit.core.importer.testexamples.annotationtypeuse.TypeUseAnnotation;
import com.tngtech.archunit.core.importer.testexamples.annotationtypeuse.TypeUseAnnotation2;
import com.tngtech.archunit.core.importer.testexamples.annotationtypeuse.TypeUseAnnotation3;

public class ClassWithFieldWithAnnotatedTypeParamOnOuterType<T> {
    public @TypeUseAnnotation ClassWithFieldWithAnnotatedTypeParamOnOuterType<@TypeUseAnnotation2 T>.@TypeUseAnnotation3 Inner fieldWithAnnotatedTypeParamOnOuterType;

    public class Inner {

    }
}
