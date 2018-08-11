package com.tngtech.archunit.core.importer.testexamples;

public class OtherClass {
    SomeEnum someEnum;

    public OtherClass(String irrelevant, SomeEnum other) {
        this.someEnum = other;
    }

    void otherMethodWithSomeEnumParameter(String irrelevant, SomeEnum someEnum) {
    }

    SomeEnum otherMethodWithSomeEnumReturnType(String irrelevant) {
        return SomeEnum.OTHER_VALUE;
    }
}
