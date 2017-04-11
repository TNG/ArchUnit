package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface GivenClassesConjunction extends GivenConjunction<JavaClass> {
    @Override
    @PublicAPI(usage = ACCESS)
    ClassesShouldConjunction should(ArchCondition<JavaClass> condition);

    @PublicAPI(usage = ACCESS)
    ClassesShould should();
}
