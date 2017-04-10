package com.tngtech.archunit.core.importer.testexamples.callimport;

public class CallsOtherConstructor {
    void createOther() {
        new CallsOwnConstructor("string");
    }
}
