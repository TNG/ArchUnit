package com.tngtech.archunit.core.importer.testexamples.dependents;

import java.io.Serializable;

public class FirstClassWithDependency {
    void first() {
        ClassHoldingDependencies instanceOne = new ClassHoldingDependencies();
        Object someOtherObject = new Object();
        Serializable someSerializable = new Serializable() {
        };
        instanceOne.setSomeInt(1);
        someOtherObject.toString();
        someSerializable.hashCode();
        instanceOne.someInt = 5;
    }

    void second() {
        ClassHoldingDependencies instanceTwo = new ClassHoldingDependencies(1);
        instanceTwo.setSomeString("string");
        instanceTwo.someString = "other";
    }
}
