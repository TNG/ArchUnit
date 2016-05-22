package com.tngtech.archunit.core.testexamples.callimport;

public class CallsOtherConstructor {
    void createOther() {
        new CallsOwnConstructor("string");
    }
}
