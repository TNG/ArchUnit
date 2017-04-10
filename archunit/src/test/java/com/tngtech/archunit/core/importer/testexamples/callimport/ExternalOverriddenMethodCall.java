package com.tngtech.archunit.core.importer.testexamples.callimport;

import com.tngtech.archunit.core.importer.testexamples.complexexternal.ChildClass;

public class ExternalOverriddenMethodCall {
    private ChildClass child;

    void call() {
        child.overrideMe();
    }
}
