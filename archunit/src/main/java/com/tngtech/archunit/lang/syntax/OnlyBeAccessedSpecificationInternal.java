package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.lang.syntax.elements.ClassesShould;
import com.tngtech.archunit.lang.syntax.elements.OnlyBeAccessedSpecification;

import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyBeAccessedByAnyPackage;

class OnlyBeAccessedSpecificationInternal implements OnlyBeAccessedSpecification {
    private final ClassesShouldInternal classesShould;

    OnlyBeAccessedSpecificationInternal(ClassesShouldInternal classesShould) {
        this.classesShould = classesShould;
    }

    @Override
    public ClassesShould byAnyPackage(String... packageIdentifiers) {
        return classesShould.copyWithCondition(onlyBeAccessedByAnyPackage(packageIdentifiers));
    }
}
