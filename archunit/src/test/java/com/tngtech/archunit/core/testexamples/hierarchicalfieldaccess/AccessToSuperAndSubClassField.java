package com.tngtech.archunit.core.testexamples.hierarchicalfieldaccess;

public class AccessToSuperAndSubClassField {
    String accessSuperClassField() {
        return new SubClassWithAccessedField().field;
    }

    int accessSubClassField() {
        return new SubClassWithAccessedField().maskedField;
    }
}
