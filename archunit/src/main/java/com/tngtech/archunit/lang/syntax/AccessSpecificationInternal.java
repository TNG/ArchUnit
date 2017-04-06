package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.syntax.elements.AccessSpecification;
import com.tngtech.archunit.lang.syntax.elements.ClassesShouldThat;

import static com.tngtech.archunit.lang.conditions.ArchConditions.accessClass;

class AccessSpecificationInternal implements AccessSpecification {
    private final ClassesShouldInternal classesShould;
    private static final String prefix = "access";

    AccessSpecificationInternal(ClassesShouldInternal classesShould) {
        this.classesShould = classesShould;
    }

    @Override
    public ClassesShouldThat classesThat() {
        return new ClassesShouldThatInternal(classesShould, new Function<DescribedPredicate<JavaClass>, ArchCondition<JavaClass>>() {
            @Override
            public ArchCondition<JavaClass> apply(DescribedPredicate<JavaClass> input) {
                return accessClass(input).as(prefix + " classes that " + input.getDescription());
            }
        });
    }
}
