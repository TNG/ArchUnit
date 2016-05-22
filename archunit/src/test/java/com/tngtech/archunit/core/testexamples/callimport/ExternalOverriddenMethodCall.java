package com.tngtech.archunit.core.testexamples.callimport;

import com.tngtech.archunit.core.testexamples.complexexternal.ChildClass;

public class ExternalOverriddenMethodCall {
    private ChildClass child;

    void call() {
        child.overrideMe();
    }
}
