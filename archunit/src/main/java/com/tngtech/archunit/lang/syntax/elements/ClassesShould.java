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

    ClassesShouldConjunction accessField(Class<?> owner, String fieldName);

    ClassesShouldConjunction accessField(String ownerName, String fieldName);

    ClassesShouldConjunction accessFieldWhere(DescribedPredicate<? super JavaFieldAccess> predicate);

    ClassesShouldConjunction getField(Class<?> owner, String fieldName);

    ClassesShouldConjunction getField(String ownerName, String fieldName);

    ClassesShouldConjunction getFieldWhere(DescribedPredicate<? super JavaFieldAccess> predicate);

    ClassesShouldConjunction setField(Class<?> owner, String fieldName);

    ClassesShouldConjunction setField(String ownerName, String fieldName);

    ClassesShouldConjunction setFieldWhere(DescribedPredicate<? super JavaFieldAccess> predicate);

    ClassesShouldConjunction callMethodWhere(DescribedPredicate<? super JavaMethodCall> predicate);
}
