package com.tngtech.archunit.core.importer.testexamples.annotationfieldimport;

import com.tngtech.archunit.core.importer.testexamples.annotationtypeuse.TypeUseAnnotation;
import com.tngtech.archunit.core.importer.testexamples.annotationtypeuse.TypeUseAnnotation2;
import com.tngtech.archunit.core.importer.testexamples.annotationtypeuse.TypeUseAnnotation3;
import com.tngtech.archunit.core.importer.testexamples.annotationtypeuse.TypeUseAnnotation4;

public class ClassWithFieldWithAnnotatedTypeParamInner {
    public @TypeUseAnnotation ClassWithFieldWithAnnotatedTypeParamInner.@TypeUseAnnotation2 Inner1<@TypeUseAnnotation3 ClassWithFieldWithAnnotatedTypeParamInner.@TypeUseAnnotation4 Inner2> fieldWithAnnotatedTypeParamInner;

    public class Inner1<T> {
    }

    public class Inner2 {
    }
}
