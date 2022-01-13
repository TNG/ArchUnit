package com.tngtech.archunit.core.domain.testexamples;

public class BReferencedByA {
    String someField;

    public BReferencedByA() {
    }

    public BReferencedByA(String someField) {
        this.someField = someField;
    }

    String getSomeField() {
        return someField;
    }

    void getNothing() {
    }
}
