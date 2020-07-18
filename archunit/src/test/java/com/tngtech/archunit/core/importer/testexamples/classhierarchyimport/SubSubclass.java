package com.tngtech.archunit.core.importer.testexamples.classhierarchyimport;

public class SubSubclass extends Subclass implements Subinterface, YetAnotherInterface {
    private String printMe = "NoOp";

    void doSomethingSubSub() {
        System.err.println(printMe);
    }
}
