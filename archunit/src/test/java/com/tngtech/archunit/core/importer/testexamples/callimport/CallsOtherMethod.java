package com.tngtech.archunit.core.importer.testexamples.callimport;

public class CallsOtherMethod {
    CallsOwnMethod other;

    String getFromOther() {
        return other.getString();
    }
}
