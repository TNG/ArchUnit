package com.tngtech.archunit.visual.testclasses;

public class SomeClass {
    private String dependencyOnString;

    public void setDependencyOnString(String dependencyOnString) {
        this.dependencyOnString = dependencyOnString.replace("foo", "bar");
    }

    public class InnerClass {
    }
}
