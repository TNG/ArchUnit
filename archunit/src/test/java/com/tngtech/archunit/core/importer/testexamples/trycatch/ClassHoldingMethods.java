package com.tngtech.archunit.core.importer.testexamples.trycatch;

public class ClassHoldingMethods {
    int someInt;
    String someString;

    ClassHoldingMethods() {
    }

    void setSomeInt(int someInt) {
        this.someInt = someInt;
    }

    void setSomeString(String someString) {
        this.someString = someString;
    }

    void doSomething() {
        setSomeInt(5);
    }
}
