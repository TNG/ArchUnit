package com.tngtech.archunit.core.importer.testexamples.callimport;

import java.util.HashMap;

import com.tngtech.archunit.core.importer.testexamples.complexexternal.ChildClass;

public class ExternalSubTypeConstructorCall {
    void call() {
        new ChildClass();
    }

    void newHashMap() {
        System.out.println(new HashMap<>());
    }
}
