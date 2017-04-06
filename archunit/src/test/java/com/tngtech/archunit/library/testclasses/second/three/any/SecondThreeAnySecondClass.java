package com.tngtech.archunit.library.testclasses.second.three.any;

import com.tngtech.archunit.library.testclasses.some.pkg.SomeFirstClass;

public class SecondThreeAnySecondClass {
    SomeFirstClass illegalTarget;

    void call() {
        illegalTarget.callMe();
    }

    public void callMe() {
    }
}
