package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;

public interface GivenClasses extends GivenObjects<JavaClass> {
    ClassesShould should();

    GivenClassesThat that();

    @Override
    GivenClassesConjunction that(DescribedPredicate<? super JavaClass> predicate);
}
