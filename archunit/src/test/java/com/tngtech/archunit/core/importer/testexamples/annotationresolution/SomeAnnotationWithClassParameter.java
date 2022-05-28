package com.tngtech.archunit.core.importer.testexamples.annotationresolution;

public @interface SomeAnnotationWithClassParameter {
    Class<?> value();
}
