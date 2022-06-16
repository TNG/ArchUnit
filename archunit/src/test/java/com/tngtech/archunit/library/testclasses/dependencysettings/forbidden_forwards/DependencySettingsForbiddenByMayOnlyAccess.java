package com.tngtech.archunit.library.testclasses.dependencysettings.forbidden_forwards;

import com.tngtech.archunit.library.testclasses.dependencysettings.forbidden_backwards.DependencySettingsForbiddenByMayOnlyBeAccessed;
import com.tngtech.archunit.library.testclasses.dependencysettings.origin.DependencySettingsOriginClass;

public class DependencySettingsForbiddenByMayOnlyAccess {
    DependencySettingsOriginClass origin;
    DependencySettingsForbiddenByMayOnlyBeAccessed forbiddenByMayOnlyBeAccessed;
}
