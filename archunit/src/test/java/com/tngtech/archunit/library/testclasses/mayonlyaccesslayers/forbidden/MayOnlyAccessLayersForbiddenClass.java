package com.tngtech.archunit.library.testclasses.mayonlyaccesslayers.forbidden;

import com.tngtech.archunit.library.testclasses.mayonlyaccesslayers.origin.MayOnlyAccessLayersOriginClass;

public class MayOnlyAccessLayersForbiddenClass {
    public void callMe() {
        new MayOnlyAccessLayersOriginClass();
    }
}
