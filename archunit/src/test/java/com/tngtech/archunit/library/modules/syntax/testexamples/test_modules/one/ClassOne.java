package com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.one;

import com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.TestAnnotation;
import com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.TestAnnotationCustomName;
import com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.two.ClassTwo;

@SuppressWarnings("unused")
@TestAnnotation(name = "one")
@TestAnnotationCustomName(customName = "customOne")
public class ClassOne {
    ClassTwo classTwo;
}
