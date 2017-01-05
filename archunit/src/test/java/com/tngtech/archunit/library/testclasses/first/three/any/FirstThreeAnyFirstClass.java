package com.tngtech.archunit.library.testclasses.first.three.any;

import com.tngtech.archunit.library.testclasses.first.any.pkg.FirstAnyFirstClass;
import com.tngtech.archunit.library.testclasses.second.three.any.SecondThreeAnySecondClass;

public class FirstThreeAnyFirstClass {
    SecondThreeAnySecondClass legalTarget;
    FirstAnyFirstClass illegalTarget;

    void call() {
        legalTarget.callMe();
        illegalTarget.callMe();
    }
}
