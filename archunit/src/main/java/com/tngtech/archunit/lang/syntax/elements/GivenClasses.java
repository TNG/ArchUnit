package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.core.JavaClass;

public interface GivenClasses extends GivenObjects<JavaClass> {
    ClassesShould should();

    GivenClassesThat that();
}
