package com.tngtech.archunit.core.importer.testexamples.fieldimport;

public class ClassWithStringField {
    @FieldAnnotation
    private String stringField;

    @interface FieldAnnotation {
    }
}
