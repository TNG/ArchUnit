package com.tngtech.archunit.core.testexamples.dependents;

import java.io.Serializable;

public class FirstClassWithDependency {
    void first() {
        ClassWithDependents instanceOne = new ClassWithDependents();
        Object someOtherObject = new Object();
        Serializable someSerializable = new Serializable() {
        };
        instanceOne.setSomeInt(1);
        someOtherObject.toString();
        someSerializable.hashCode();
        instanceOne.someInt = 5;
    }

    void second() {
        ClassWithDependents instanceTwo = new ClassWithDependents(1);
        instanceTwo.setSomeString("string");
        instanceTwo.someString = "other";
    }
}
