package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface GivenClassesConjunction extends GivenConjunction<JavaClass> {
    @Override
    @PublicAPI(usage = ACCESS)
    ClassesShouldConjunction should(ArchCondition<JavaClass> condition);

    @PublicAPI(usage = ACCESS)
    ClassesShould should();

    @Override
    @PublicAPI(usage = ACCESS)
    GivenClassesConjunction and(DescribedPredicate<? super JavaClass> predicate);

    /**
     * @see #and(DescribedPredicate)
     */
    @PublicAPI(usage = ACCESS)
    GivenClassesThat and();

    @Override
    @PublicAPI(usage = ACCESS)
    GivenClassesConjunction or(DescribedPredicate<? super JavaClass> predicate);

    /**
     * @see #or(DescribedPredicate)
     */
    @PublicAPI(usage = ACCESS)
    GivenClassesThat or();
}
