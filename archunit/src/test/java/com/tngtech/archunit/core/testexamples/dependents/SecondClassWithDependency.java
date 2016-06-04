package com.tngtech.archunit.core.testexamples.dependents;

public class SecondClassWithDependency {
    void other() {
        ClassWithDependents instanceOne = new ClassWithDependents();
        instanceOne.setSomeInt(0);
        instanceOne.someInt = 99;
    }
}
