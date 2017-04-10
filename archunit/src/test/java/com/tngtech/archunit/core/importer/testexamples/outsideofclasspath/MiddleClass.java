package com.tngtech.archunit.core.importer.testexamples.outsideofclasspath;

public class MiddleClass extends MissingSuperClass {
    public String someField;
    private MissingDependency missingDependency = new MissingDependency();

    public MiddleClass() {
    }

    @Override
    public void overrideMe() {
        System.out.println(missingDependency.getSomeString());
    }
}
