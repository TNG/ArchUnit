package com.tngtech.archunit.core.testexamples.classhierarchyimport;

public class SubSubClass extends SubClass {
    private String printMe = "NoOp";

    void doSomethingSubSub() {
        System.err.println(printMe);
    }
}
