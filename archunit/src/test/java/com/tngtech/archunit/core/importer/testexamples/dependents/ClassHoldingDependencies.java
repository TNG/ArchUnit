package com.tngtech.archunit.core.importer.testexamples.dependents;

public class ClassHoldingDependencies {
    int someInt;
    String someString;

    public ClassHoldingDependencies() {
    }

    public ClassHoldingDependencies(int someInt) {
        this();
        this.someInt = someInt;
    }

    public void setSomeInt(int someInt) {
        this.someInt = someInt;
    }

    public void setSomeString(String someString) {
        this.someString = someString;
    }

    void doSomething() {
        setSomeInt(5);
    }
}
