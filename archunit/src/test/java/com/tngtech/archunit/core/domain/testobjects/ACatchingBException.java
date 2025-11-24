package com.tngtech.archunit.core.domain.testobjects;

@SuppressWarnings("unused")
public class ACatchingBException {
    static {
        try {
            new AThrowingBException();
        } catch (BException1 e) {
        }
    }

    public ACatchingBException() {
        try {
            new AThrowingBException();
        } catch (BException1 e) {
        }
    }

    void method() {
        try {
            new AThrowingBException();
        } catch (BException1 e) {
        }
    }
}
