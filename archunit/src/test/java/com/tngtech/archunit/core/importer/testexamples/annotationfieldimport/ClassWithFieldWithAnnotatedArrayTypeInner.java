package com.tngtech.archunit.core.importer.testexamples.annotationfieldimport;

import com.tngtech.archunit.core.importer.testexamples.annotationtypeuse.TypeUseAnnotation;
import com.tngtech.archunit.core.importer.testexamples.annotationtypeuse.TypeUseAnnotation2;
import com.tngtech.archunit.core.importer.testexamples.annotationtypeuse.TypeUseAnnotation3;

public class ClassWithFieldWithAnnotatedArrayTypeInner {
    public @TypeUseAnnotation ClassWithFieldWithAnnotatedArrayTypeInner.@TypeUseAnnotation2 Inner @TypeUseAnnotation3 [] fieldWithAnnotatedArrayTypeInner;

    public class Inner {

    }
}
