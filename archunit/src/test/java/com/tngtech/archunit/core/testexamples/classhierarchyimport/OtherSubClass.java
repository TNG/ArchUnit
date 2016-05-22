package com.tngtech.archunit.core.testexamples.classhierarchyimport;

public class OtherSubClass extends BaseClass {
    private int foo = 5;

    void soSthOtherSub() {
        for (int i = 0; i < foo; i++) {
            System.err.println("Foo");
        }
    }
}
