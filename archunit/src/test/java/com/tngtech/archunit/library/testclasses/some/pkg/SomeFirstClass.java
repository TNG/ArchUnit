package com.tngtech.archunit.library.testclasses.some.pkg;

import com.tngtech.archunit.library.testclasses.some.pkg.sub.SomeSecondClass;

public class SomeFirstClass {
    SomeSecondClass legalTarget;

    void call() {
        legalTarget.callMe();
    }

    public void callMe() {
    }
}
