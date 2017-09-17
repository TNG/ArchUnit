package com.tngtech.archunit.library.testclasses.some.pkg.sub;

import com.tngtech.archunit.library.testclasses.first.any.pkg.FirstAnyPkgClass;

public class SomePkgSubClass {
    FirstAnyPkgClass legalTarget;

    void call() {
        legalTarget.callMe();
    }

    public void callMe() {
    }
}
