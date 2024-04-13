package com.tngtech.archunit.core.importer.testexamples.outsideofclasspath;

public class MiddleClass extends MissingSuperclass {
    public String someField;
    private MissingDependency missingDependency = new MissingDependency();

    public MiddleClass() {
    }

    @Override
    public void overrideMe() {
        System.out.println(missingDependency.getSomeString());
    }
}
