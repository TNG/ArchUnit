package com.tngtech.archunit.visual.testclasses;

public class ThirdClass {
    private SomeClass someClass;

    void call() {
        someClass.setDependencyOnString("foo");
    }
}
