package com.tngtech.archunit.lang.syntax.elements;

public interface ClassesShould {
    AccessSpecification access();

    OnlyBeAccessedSpecification<ShouldConjunction> onlyBeAccessed();

    /**
     * @see com.tngtech.archunit.base.PackageMatcher
     */
    ShouldConjunction resideInAPackage(String packageIdentifier);
}
