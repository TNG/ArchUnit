package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.PackageMatcher;
import com.tngtech.archunit.core.domain.JavaClass;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface OnlyBeAccessedSpecification<CONJUNCTION> {
    /**
     * Matches classes residing in a package matching any of the supplied package identifiers.
     *
     * @param packageIdentifiers Strings identifying packages, for details see {@link PackageMatcher}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION byAnyPackage(String... packageIdentifiers);

    /**
     * @return A syntax element that allows restricting which classes the access should be from
     */
    @PublicAPI(usage = ACCESS)
    ClassesShouldThat byClassesThat();

    /**
     * @param predicate Restricts which classes the access should be from
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION byClassesThat(DescribedPredicate<? super JavaClass> predicate);
}
