package com.tngtech.archunit.library.testclasses.mayonlyaccesslayers.origin;

import com.tngtech.archunit.library.testclasses.mayonlyaccesslayers.allowed.MayOnlyAccessLayersAllowedClass;
import com.tngtech.archunit.library.testclasses.mayonlyaccesslayers.forbidden.MayOnlyAccessLayersForbiddenClass;

public class MayOnlyAccessLayersOriginClass {
    MayOnlyAccessLayersAllowedClass legalTarget;
    MayOnlyAccessLayersForbiddenClass illegalTarget;

    @SuppressWarnings("unused")
    void call() {
        legalTarget.callMe();
        illegalTarget.callMe();
    }
}
