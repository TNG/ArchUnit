package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.core.JavaClass;

public interface GivenClassesConjunction extends GivenConjunction<JavaClass> {
    ClassesShould should();
}
