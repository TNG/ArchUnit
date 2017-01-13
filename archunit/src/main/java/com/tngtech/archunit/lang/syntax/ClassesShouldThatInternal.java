package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.lang.syntax.elements.ClassesShouldThat;

import static com.tngtech.archunit.lang.conditions.ArchConditions.accessClassesThatResideIn;

class ClassesShouldThatInternal extends ClassesShouldInternal implements ClassesShouldThat {
    ClassesShouldThatInternal(ClassesShouldInternal classesShould) {
        super(classesShould);
    }

    @Override
    public ClassesShouldThat resideInPackage(String packageIdentifier) {
        return new ClassesShouldThatInternal(copyWithCondition(accessClassesThatResideIn(packageIdentifier)));
    }
}
