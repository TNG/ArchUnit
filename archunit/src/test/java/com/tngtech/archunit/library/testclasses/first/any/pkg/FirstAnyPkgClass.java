package com.tngtech.archunit.library.testclasses.first.any.pkg;

import com.tngtech.archunit.library.testclasses.second.three.any.SecondThreeAnyClass;
import com.tngtech.archunit.library.testclasses.some.pkg.sub.SomePkgSubClass;

@SuppressWarnings("WeakerAccess")
public class FirstAnyPkgClass {
    SomePkgSubClass illegalTarget;
    SecondThreeAnyClass legalTarget;
    // The importer will never find the bytecode of FirstAnyPkgClass[].class, thus it will always be a stub in the same package
    FirstAnyPkgClass[] evilArrayToCauseOriginallyUnimportedClass;
    ClassOnlyDependentOnOwnPackageAndObject classOnlyDependentOnOwnPackageAndObject;

    void call() {
        illegalTarget.callMe();
        legalTarget.callMe();
    }

    public void callMe() {
    }
}
