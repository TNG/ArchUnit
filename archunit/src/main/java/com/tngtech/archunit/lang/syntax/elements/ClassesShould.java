package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.core.JavaMethodCall;

public interface ClassesShould {
    AccessSpecification access();

    OnlyBeAccessedSpecification<ClassesShouldConjunction> onlyBeAccessed();

    ClassesShouldConjunction beNamed(String name);

    /**
     * @see com.tngtech.archunit.base.PackageMatcher
     */
    ClassesShouldConjunction resideInAPackage(String packageIdentifier);

    ClassesShouldConjunction setFieldWhere(DescribedPredicate<? super JavaFieldAccess> predicate);

    ClassesShouldConjunction callMethodWhere(DescribedPredicate<? super JavaMethodCall> predicate);
}
