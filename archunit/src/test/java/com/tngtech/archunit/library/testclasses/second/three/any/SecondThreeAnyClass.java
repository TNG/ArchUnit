package com.tngtech.archunit.library.testclasses.second.three.any;

import com.tngtech.archunit.library.testclasses.some.pkg.SomePkgClass;

public class SecondThreeAnyClass {
    SomePkgClass illegalTarget;

    void call() {
        illegalTarget.callMe();
    }

    public void callMe() {
    }
}
