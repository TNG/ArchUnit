package com.tngtech.archunit.core.importer.testexamples.fieldaccessimport;

public class ForeignFieldAccessFromStaticInitializer {
    static {
        String stringValue = new OwnFieldAccess().stringValue;
        new OwnFieldAccess().intValue = 0;
    }
}
