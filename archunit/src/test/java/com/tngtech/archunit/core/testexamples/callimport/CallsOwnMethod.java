package com.tngtech.archunit.core.testexamples.callimport;

public class CallsOwnMethod {

    public String getString() {
        return string();
    }

    private String string() {
        return "string";
    }
}
