package com.tngtech.archunit.core.importer.testexamples.annotationfieldimport;

import java.util.List;

import com.tngtech.archunit.core.importer.testexamples.annotationtypeuse.TypeUseAnnotation;
import com.tngtech.archunit.core.importer.testexamples.annotationtypeuse.TypeUseAnnotation2;

public class ClassWithFieldWithAnnotatedLowerWildCardBoundInner {
    public List<? super @TypeUseAnnotation @TypeUseAnnotation2 Inner> fieldWithAnnotatedLowerWildCardBoundInner;

    public class Inner {

    }
}
