package com.tngtech.archunit.core.importer.testexamples.synthetic.constructors;

public class SyntheticConstructor {
    private SyntheticConstructor() {
    }

    public class InnerClass {
        InnerClass() {
            new SyntheticConstructor();
        }
    }
}
