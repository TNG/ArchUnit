package com.tngtech.archunit.library.testclasses.some.pkg;

import com.tngtech.archunit.library.testclasses.some.pkg.sub.SomePkgSubclass;

public class SomePkgClass {
    SomePkgSubclass legalTarget;

    void call() {
        legalTarget.callMe();
    }

    public void callMe() {
    }
}
