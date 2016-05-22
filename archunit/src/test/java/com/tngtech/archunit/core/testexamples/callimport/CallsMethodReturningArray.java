package com.tngtech.archunit.core.testexamples.callimport;

public class CallsMethodReturningArray {
    SomeEnum[] getValues() {
        return SomeEnum.values();
    }

    public enum SomeEnum {
        ONE, TWO
    }
}
