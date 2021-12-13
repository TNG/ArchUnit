package com.tngtech.archunit.library.testclasses.mayonlyaccesslayers.allowed;

import com.tngtech.archunit.library.testclasses.mayonlyaccesslayers.origin.MayOnlyAccessLayersOriginClass;

public class MayOnlyAccessLayersAllowedClass {
    public void callMe() {
        new MayOnlyAccessLayersOriginClass();
    }
}
