package com.tngtech.archunit.library.modules.testexamples.valid.module1.sub1;

import java.util.List;

import com.tngtech.archunit.library.modules.testexamples.valid.module1.sub2.FirstClassInSubModule12;
import com.tngtech.archunit.library.modules.testexamples.valid.module1.sub2.SecondClassInSubModule12;

@SuppressWarnings("unused")
public class FirstClassInSubModule11 {
    SecondClassInSubModule11 noDependencyToOtherModule;
    SecondClassInSubModule11[] noDependencyToOtherModuleByArrayType;
    FirstClassInSubModule12[] firstDependencyOnSubModule12;
    SecondClassInSubModule12[][] secondDependencyOnSubModule12;

    String firstUndefinedDependency;
    List<?> secondUndefinedDependency;
}
