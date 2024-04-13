package com.tngtech.archunit.core.importer.testexamples.outsideofclasspath;

public class MissingDependency {
    public String someField;

    public MissingDependency() {
    }

    public String getSomeString() {
        return someField;
    }
}
