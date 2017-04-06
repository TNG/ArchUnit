package com.tngtech.archunit.library.testclasses.second.any.pkg;

import com.tngtech.archunit.library.testclasses.first.any.pkg.FirstAnyFirstClass;

public class SecondAnySecondClass {
    FirstAnyFirstClass legalTarget;

    void call() {
        legalTarget.callMe();
    }
}
