package com.tngtech.archunit.core.importer.testexamples.integration;

public class ClassBDependingOnClassA {
    private String bState = "initialized";
    private final ClassA classA;

    public ClassBDependingOnClassA() {
        classA = new ClassA(bState);
    }

    public String getStateFromA() {
        return classA.getState();
    }
}
