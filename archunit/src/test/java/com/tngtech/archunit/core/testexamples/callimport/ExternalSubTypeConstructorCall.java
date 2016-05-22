package com.tngtech.archunit.core.testexamples.callimport;

import com.tngtech.archunit.core.testexamples.complexexternal.ChildClass;

public class ExternalSubTypeConstructorCall {
    void call() {
        new ChildClass();
    }
}
