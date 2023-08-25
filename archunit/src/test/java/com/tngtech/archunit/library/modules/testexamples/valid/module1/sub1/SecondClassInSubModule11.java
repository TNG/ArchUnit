package com.tngtech.archunit.library.modules.testexamples.valid.module1.sub1;

import java.util.Collection;

import com.tngtech.archunit.library.modules.testexamples.valid.module2.FirstClassInModule2;
import com.tngtech.archunit.library.modules.testexamples.valid.module2.sub1.FirstClassInSubModule21;

@SuppressWarnings("unused")
public class SecondClassInSubModule11 {
    FirstClassInModule2 dependencyOnModule2;
    FirstClassInSubModule21 dependencyOnSubModule21;

    Collection<?> thirdUndefinedDependency;
}
