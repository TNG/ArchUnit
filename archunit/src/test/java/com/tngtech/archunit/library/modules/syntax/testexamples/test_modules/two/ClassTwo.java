package com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.two;

import com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.TestAnnotation;
import com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.TestAnnotationCustomName;

@TestAnnotation(name = "two")
@TestAnnotationCustomName(customName = "customTwo")
public class ClassTwo {
}
