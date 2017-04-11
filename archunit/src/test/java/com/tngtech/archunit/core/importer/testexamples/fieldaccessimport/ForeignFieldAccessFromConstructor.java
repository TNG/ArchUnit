package com.tngtech.archunit.core.importer.testexamples.fieldaccessimport;

public class ForeignFieldAccessFromConstructor {
    public ForeignFieldAccessFromConstructor() {
        String stringValue = new OwnFieldAccess().stringValue;
        new OwnFieldAccess().intValue = 0;
    }
}
