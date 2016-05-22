package com.tngtech.archunit.core.testexamples.fieldaccessimport;

public class ForeignFieldAccessFromStaticInitializer {
    static {
        String stringValue = new OwnFieldAccess().stringValue;
        new OwnFieldAccess().intValue = 0;
    }
}
