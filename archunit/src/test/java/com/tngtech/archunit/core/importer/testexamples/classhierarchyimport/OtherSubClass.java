package com.tngtech.archunit.core.importer.testexamples.classhierarchyimport;

public class OtherSubClass extends BaseClass implements ParentInterface {
    private int foo = 5;

    void soSthOtherSub() {
        for (int i = 0; i < foo; i++) {
            System.err.println("Foo");
        }
    }
}
