package com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.one;

import com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.TestAnnotation;
import com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.TestAnnotationCustomName;

@TestAnnotation(name = "one")
@TestAnnotationCustomName(customName = "customOne")
public class ClassOne {
}
