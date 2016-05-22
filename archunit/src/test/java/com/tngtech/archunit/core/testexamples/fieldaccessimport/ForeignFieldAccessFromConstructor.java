package com.tngtech.archunit.core.testexamples.fieldaccessimport;

public class ForeignFieldAccessFromConstructor {
    public ForeignFieldAccessFromConstructor() {
        String stringValue = new OwnFieldAccess().stringValue;
        new OwnFieldAccess().intValue = 0;
    }
}
