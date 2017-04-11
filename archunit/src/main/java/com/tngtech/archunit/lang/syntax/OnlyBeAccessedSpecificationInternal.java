package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.archunit.lang.syntax.elements.ClassesShouldConjunction;
import com.tngtech.archunit.lang.syntax.elements.ClassesShouldThat;
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

    @Override
    public ClassesShouldThat byClassesThat() {
        return new ClassesShouldThatInternal(classesShould, new Function<DescribedPredicate<JavaClass>, ArchCondition<JavaClass>>() {
            @Override
            public ArchCondition<JavaClass> apply(DescribedPredicate<JavaClass> predicate) {
                return ArchConditions.onlyBeAccessedByClassesThat(predicate);
            }
        });
    }
}
