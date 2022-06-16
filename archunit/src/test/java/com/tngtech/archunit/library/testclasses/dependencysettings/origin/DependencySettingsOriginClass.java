package com.tngtech.archunit.library.testclasses.dependencysettings.origin;

import com.tngtech.archunit.library.testclasses.dependencysettings.allowed.Allowed;
import com.tngtech.archunit.library.testclasses.dependencysettings.forbidden_forwards.DependencySettingsForbiddenByMayOnlyAccess;
import com.tngtech.archunit.library.testclasses.dependencysettings_outside.DependencySettingsOutsideOfLayersBeingAccessedByLayers;

public class DependencySettingsOriginClass {
    Allowed allowed;
    DependencySettingsForbiddenByMayOnlyAccess forbiddenByMayOnlyAccess;
    DependencySettingsOutsideOfLayersBeingAccessedByLayers beingAccessedByLayers;
}
