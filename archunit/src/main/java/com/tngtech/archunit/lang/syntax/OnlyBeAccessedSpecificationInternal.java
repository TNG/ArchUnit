package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.lang.syntax.elements.OnlyBeAccessedSpecification;
import com.tngtech.archunit.lang.syntax.elements.ShouldConjunction;

import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyBeAccessedByAnyPackage;

class OnlyBeAccessedSpecificationInternal implements OnlyBeAccessedSpecification<ShouldConjunction> {
    private final ClassesShouldInternal classesShould;

    OnlyBeAccessedSpecificationInternal(ClassesShouldInternal classesShould) {
        this.classesShould = classesShould;
    }

    @Override
    public ShouldConjunction byAnyPackage(String... packageIdentifiers) {
        return classesShould.copyWithCondition(onlyBeAccessedByAnyPackage(packageIdentifiers));
    }
}
