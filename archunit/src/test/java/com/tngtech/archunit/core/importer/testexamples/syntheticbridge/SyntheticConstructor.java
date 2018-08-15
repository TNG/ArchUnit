package com.tngtech.archunit.core.importer.testexamples.syntheticbridge;

public class SyntheticConstructor {
    private SyntheticConstructor() {
    }

    class InnerClass {
        InnerClass() {
            new SyntheticConstructor();
        }
    }
}
