package com.tngtech.archunit.core.importer.testexamples.hierarchicalfieldaccess;

public class AccessToSuperAndSubClassField {
    String accessSuperClassField() {
        return new SubClassWithAccessedField().field;
    }

    int accessSubClassField() {
        return new SubClassWithAccessedField().maskedField;
    }
}
