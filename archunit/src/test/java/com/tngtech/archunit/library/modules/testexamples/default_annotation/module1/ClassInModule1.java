package com.tngtech.archunit.library.modules.testexamples.default_annotation.module1;

import com.tngtech.archunit.library.modules.testexamples.default_annotation.module2.ClassInModule2;
import com.tngtech.archunit.library.modules.testexamples.default_annotation.module3.ClassInModule3;

@SuppressWarnings("unused")
public class ClassInModule1 {
    ClassInModule3 forbiddenDependencyBecauseWrongModule;
    ClassInModule2 forbiddenDependencyBecauseWrongPackage;
}
