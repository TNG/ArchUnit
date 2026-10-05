package com.tngtech.archunit.core.importer.testexamples.annotationfieldimport;

import java.util.List;

import com.tngtech.archunit.core.importer.testexamples.annotationtypeuse.TypeUseAnnotation;
import com.tngtech.archunit.core.importer.testexamples.annotationtypeuse.TypeUseAnnotation2;

public class ClassWithFieldWithAnnotatedUpperWildCardBoundInner {
    public List<? extends @TypeUseAnnotation @TypeUseAnnotation2 Inner> fieldWithAnnotatedUpperWildCardBoundInner;

    public class Inner {

    }
}
