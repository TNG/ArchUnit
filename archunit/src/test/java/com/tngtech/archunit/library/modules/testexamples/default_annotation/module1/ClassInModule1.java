package com.tngtech.archunit.library.modules.testexamples.default_annotation.module1;

import com.tngtech.archunit.library.modules.testexamples.default_annotation.module2.InternalClassInModule2;
import com.tngtech.archunit.library.modules.testexamples.default_annotation.module2.api.ApiClassInModule2;
import com.tngtech.archunit.library.modules.testexamples.default_annotation.module3.ClassInModule3;

@SuppressWarnings("unused")
public class ClassInModule1 {
    ApiClassInModule2 allowedDependency;
    ClassInModule3 forbiddenDependencyBecauseWrongModule;
    InternalClassInModule2 forbiddenDependencyBecauseWrongPackage;
}
