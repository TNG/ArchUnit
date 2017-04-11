package com.tngtech.archunit.core.importer.testexamples.innerclassimport;

public class CalledClass {
    private String someString;

    CalledClass() {
    }

    public CalledClass(String someString) {
        this.someString = someString;
    }

    public void doIt() {
    }
}
