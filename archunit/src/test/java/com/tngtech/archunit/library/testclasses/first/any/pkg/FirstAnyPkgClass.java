package com.tngtech.archunit.library.testclasses.first.any.pkg;

import com.tngtech.archunit.library.testclasses.second.three.any.SecondThreeAnyClass;
import com.tngtech.archunit.library.testclasses.some.pkg.sub.SomePkgSubClass;

public class FirstAnyPkgClass {
    SomePkgSubClass illegalTarget;
    SecondThreeAnyClass legalTarget;

    void call() {
        illegalTarget.callMe();
        legalTarget.callMe();
    }

    public void callMe() {
    }
}
