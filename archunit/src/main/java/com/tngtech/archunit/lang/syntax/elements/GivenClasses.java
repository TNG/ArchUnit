package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.JavaClass;

public interface GivenClasses extends GivenObjects<JavaClass> {
    ClassesShould should();

    GivenClassesThat that();

    GivenClasses that(DescribedPredicate<? super JavaClass> predicate);
}
