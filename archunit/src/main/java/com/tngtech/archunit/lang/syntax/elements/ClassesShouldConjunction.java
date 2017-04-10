package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;

public interface ClassesShouldConjunction extends ArchRule {
    ClassesShouldConjunction andShould(ArchCondition<? super JavaClass> condition);

    ClassesShould andShould();

    ClassesShouldConjunction orShould(ArchCondition<? super JavaClass> condition);

    ClassesShould orShould();
}
