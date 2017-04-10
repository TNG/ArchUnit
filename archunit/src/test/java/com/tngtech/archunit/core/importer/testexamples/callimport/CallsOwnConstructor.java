package com.tngtech.archunit.core.importer.testexamples.callimport;

public class CallsOwnConstructor {
    public CallsOwnConstructor(String string) {
    }

    CallsOwnConstructor copy() {
        return new CallsOwnConstructor("string");
    }
}
