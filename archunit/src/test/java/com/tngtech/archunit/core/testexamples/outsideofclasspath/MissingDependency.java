package com.tngtech.archunit.core.testexamples.outsideofclasspath;

public class MissingDependency {
    public String someField;

    public MissingDependency() {
    }

    public String getSomeString() {
        return someField;
    }
}
