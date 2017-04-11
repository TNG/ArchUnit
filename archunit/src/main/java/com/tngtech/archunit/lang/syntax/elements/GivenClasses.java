package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface GivenClasses extends GivenObjects<JavaClass> {
    @PublicAPI(usage = ACCESS)
    ClassesShould should();

    @PublicAPI(usage = ACCESS)
    GivenClassesThat that();

    @Override
    GivenClassesConjunction that(DescribedPredicate<? super JavaClass> predicate);
}
