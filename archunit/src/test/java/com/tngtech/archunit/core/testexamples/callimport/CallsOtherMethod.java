package com.tngtech.archunit.core.testexamples.callimport;

public class CallsOtherMethod {
    CallsOwnMethod other;

    String getFromOther() {
        return other.getString();
    }
}
