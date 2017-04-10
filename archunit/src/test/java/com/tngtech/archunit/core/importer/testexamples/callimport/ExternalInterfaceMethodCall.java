package com.tngtech.archunit.core.importer.testexamples.callimport;

import java.util.SortedMap;

public class ExternalInterfaceMethodCall {
    private SortedMap<Object, Object> child;

    void call() {
        child.put(new Object(), new Object());
    }
}
