package com.tngtech.archunit.core.testexamples.callimport;

import java.util.SortedMap;

public class ExternalInterfaceMethodCall {
    private SortedMap<Object, Object> child;

    void call() {
        child.put(new Object(), new Object());
    }
}
