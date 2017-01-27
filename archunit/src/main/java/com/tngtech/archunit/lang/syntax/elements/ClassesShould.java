package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.JavaFieldAccess;

public interface ClassesShould {
    AccessSpecification access();

    OnlyBeAccessedSpecification<ShouldConjunction> onlyBeAccessed();

    /**
     * @see com.tngtech.archunit.base.PackageMatcher
     */
    ShouldConjunction resideInAPackage(String packageIdentifier);

    ShouldConjunction setFieldWhere(DescribedPredicate<? super JavaFieldAccess> predicate);
}
