package com.tngtech.archunit.core.importer.testexamples.dependents;

public class SecondClassWithDependency {
    void other() {
        ClassHoldingDependencies instanceOne = new ClassHoldingDependencies();
        instanceOne.setSomeInt(0);
        instanceOne.someInt = 99;
    }
}
