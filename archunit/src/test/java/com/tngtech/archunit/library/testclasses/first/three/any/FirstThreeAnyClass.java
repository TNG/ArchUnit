package com.tngtech.archunit.library.testclasses.first.three.any;

import com.tngtech.archunit.library.testclasses.first.any.pkg.FirstAnyPkgClass;
import com.tngtech.archunit.library.testclasses.second.three.any.SecondThreeAnyClass;

public class FirstThreeAnyClass {
    SecondThreeAnyClass legalTarget;
    FirstAnyPkgClass illegalTarget;

    void call() {
        legalTarget.callMe();
        illegalTarget.callMe();
    }
}
