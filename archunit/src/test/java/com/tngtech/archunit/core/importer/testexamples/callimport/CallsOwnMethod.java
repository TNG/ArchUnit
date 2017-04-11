package com.tngtech.archunit.core.importer.testexamples.callimport;

public class CallsOwnMethod {

    public String getString() {
        return string();
    }

    private String string() {
        return "string";
    }
}
