package com.tngtech.archunit.library.testclasses.second.any.pkg;

import com.tngtech.archunit.library.testclasses.first.any.pkg.FirstAnyPkgClass;

public class SecondAnyClass {
    FirstAnyPkgClass legalTarget;

    void call() {
        legalTarget.callMe();
    }
}
