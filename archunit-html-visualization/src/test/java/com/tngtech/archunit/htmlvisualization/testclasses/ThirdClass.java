package com.tngtech.archunit.htmlvisualization.testclasses;

public class ThirdClass {
    private SomeClass someClass;

    void call() {
        someClass.setDependencyOnString("foo");
    }
}
