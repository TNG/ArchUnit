package com.tngtech.archunit.visual.testclasses;

import com.tngtech.archunit.visual.testdependencies.TestDependencyClass;
import com.tngtech.archunit.visual.testdependencies.TestDependencyClassWithInnerClass;

public class SomeClass {
    private String dependencyOnString;

    public void setDependencyOnString(String dependencyOnString) {
        this.dependencyOnString = dependencyOnString.replace("foo", "bar");
        new TestDependencyClass().targetMethod();
        new TestDependencyClassWithInnerClass.TestDependencyInnerClass().targetMethod();
    }

    public class InnerClass {
    }
}
