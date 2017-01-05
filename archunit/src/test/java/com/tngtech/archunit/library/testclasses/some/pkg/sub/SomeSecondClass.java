package com.tngtech.archunit.library.testclasses.some.pkg.sub;

import com.tngtech.archunit.library.testclasses.first.any.pkg.FirstAnyFirstClass;

public class SomeSecondClass {
    FirstAnyFirstClass legalTarget;

    void call() {
        legalTarget.callMe();
    }

    public void callMe() {
    }
}
