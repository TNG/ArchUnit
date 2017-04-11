package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface ClassesShouldConjunction extends ArchRule {
    @PublicAPI(usage = ACCESS)
    ClassesShouldConjunction andShould(ArchCondition<? super JavaClass> condition);

    @PublicAPI(usage = ACCESS)
    ClassesShould andShould();

    @PublicAPI(usage = ACCESS)
    ClassesShouldConjunction orShould(ArchCondition<? super JavaClass> condition);

    @PublicAPI(usage = ACCESS)
    ClassesShould orShould();
}
