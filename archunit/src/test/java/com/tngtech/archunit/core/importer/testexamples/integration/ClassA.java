package com.tngtech.archunit.core.importer.testexamples.integration;

public class ClassA implements InterfaceOfClassA {
    private String init;
    private String state;
    int accessibleField = 5;

    public ClassA(String init) {
        this.init = init;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
