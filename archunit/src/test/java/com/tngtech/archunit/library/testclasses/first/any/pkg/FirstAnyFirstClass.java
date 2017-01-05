package com.tngtech.archunit.library.testclasses.first.any.pkg;

import com.tngtech.archunit.library.testclasses.second.three.any.SecondThreeAnySecondClass;
import com.tngtech.archunit.library.testclasses.some.pkg.sub.SomeSecondClass;

public class FirstAnyFirstClass {
    SomeSecondClass illegalTarget;
    SecondThreeAnySecondClass legalTarget;

    void call() {
        illegalTarget.callMe();
        legalTarget.callMe();
    }

    public void callMe() {
    }
}
