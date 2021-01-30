package com.tngtech.archunit.core.domain.testobjects;

@SuppressWarnings("unused")
public class ClassWithArrayDependencies {
    private void oneDimArray() {
        new String[0].clone();
    }

    private void multiDimArray() {
        new String[0][0].clone();
    }
}
