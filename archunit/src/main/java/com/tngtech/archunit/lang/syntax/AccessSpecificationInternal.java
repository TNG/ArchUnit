package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.lang.syntax.elements.AccessSpecification;
import com.tngtech.archunit.lang.syntax.elements.ClassesShouldThat;

class AccessSpecificationInternal implements AccessSpecification {
    private final ClassesShouldInternal classesShould;

    AccessSpecificationInternal(ClassesShouldInternal classesShould) {
        this.classesShould = classesShould;
    }

    @Override
    public ClassesShouldThat classesThat() {
        return new ClassesShouldThatInternal(classesShould);
    }
}
