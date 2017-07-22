package com.tngtech.archunit.library.testclasses.some.pkg;

import com.tngtech.archunit.library.testclasses.some.pkg.sub.SomePkgSubClass;

public class SomePkgClass {
    SomePkgSubClass legalTarget;

    void call() {
        legalTarget.callMe();
    }

    public void callMe() {
    }
}
