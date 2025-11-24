package com.tngtech.archunit.library.testclasses.first.any.pkg;

import com.tngtech.archunit.library.testclasses.some.pkg.SomePkgException;

public class ClassWithCatch {
    void method() {
        try {
            callForTry();
        } catch (SomePkgException e) {
        }
    }

    void callForTry() {
    }
}
