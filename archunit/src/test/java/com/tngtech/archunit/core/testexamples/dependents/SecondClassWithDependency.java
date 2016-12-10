package com.tngtech.archunit.core.testexamples.dependents;

public class SecondClassWithDependency {
    void other() {
        ClassHoldingDependencies instanceOne = new ClassHoldingDependencies();
        instanceOne.setSomeInt(0);
        instanceOne.someInt = 99;
    }
}
