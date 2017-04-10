package com.tngtech.archunit.core.importer.testexamples.fieldaccessimport;

public class MultipleFieldAccessInSameMethod {
    void multipleCalls() {
        OwnFieldAccess other = new OwnFieldAccess();
        other.stringValue = "changed";
        if ("changed".equals(other.stringValue)) {
            other.stringValue = "changedAgain";
        }
        if (other.intValue == 0) {
            other.intValue = 10;
        }
    }
}
