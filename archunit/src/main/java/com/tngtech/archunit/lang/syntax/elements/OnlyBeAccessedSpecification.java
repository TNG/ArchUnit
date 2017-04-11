package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.PackageMatcher;

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

    @PublicAPI(usage = ACCESS)
    ClassesShouldThat byClassesThat();
}
