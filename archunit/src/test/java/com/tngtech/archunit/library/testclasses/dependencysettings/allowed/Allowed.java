package com.tngtech.archunit.library.testclasses.dependencysettings.allowed;

import com.tngtech.archunit.library.testclasses.dependencysettings.forbidden_forwards.DependencySettingsForbiddenByMayOnlyAccess;

public class Allowed {
    DependencySettingsForbiddenByMayOnlyAccess notForbiddenFromHere;
}
