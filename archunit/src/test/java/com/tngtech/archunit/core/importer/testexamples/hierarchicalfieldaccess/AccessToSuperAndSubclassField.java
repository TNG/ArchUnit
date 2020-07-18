package com.tngtech.archunit.core.importer.testexamples.hierarchicalfieldaccess;

public class AccessToSuperAndSubclassField {
    String accessSuperclassField() {
        return new SubclassWithAccessedField().field;
    }

    int accessSubclassField() {
        return new SubclassWithAccessedField().maskedField;
    }
}
