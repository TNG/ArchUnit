package com.tngtech.archunit.core.testexamples.dependents;

public class ClassWithDependents {
    int someInt;
    String someString;

    public ClassWithDependents() {
    }

    public ClassWithDependents(int someInt) {
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
