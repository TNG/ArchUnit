package com.tngtech.archunit.core.testexamples.dependents;

public class ClassDependingOnParentThroughChild {
    SubClassHoldingDependencies classHoldingDependencies;

    void doSomething() {
        classHoldingDependencies = new SubClassHoldingDependencies();
        classHoldingDependencies.parentField = new Object();
        classHoldingDependencies.parentMethod();
    }
}
