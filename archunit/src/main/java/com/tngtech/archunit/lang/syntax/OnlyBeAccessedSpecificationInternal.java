package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.lang.syntax.elements.ClassesShouldConjunction;
import com.tngtech.archunit.lang.syntax.elements.OnlyBeAccessedSpecification;

import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyBeAccessedByAnyPackage;

class OnlyBeAccessedSpecificationInternal implements OnlyBeAccessedSpecification<ClassesShouldConjunction> {
    private final ClassesShouldInternal classesShould;

    OnlyBeAccessedSpecificationInternal(ClassesShouldInternal classesShould) {
        this.classesShould = classesShould;
    }

    @Override
    public ClassesShouldConjunction byAnyPackage(String... packageIdentifiers) {
        return classesShould.addCondition(onlyBeAccessedByAnyPackage(packageIdentifiers));
    }
}
