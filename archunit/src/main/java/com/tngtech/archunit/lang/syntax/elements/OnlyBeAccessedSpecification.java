package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.base.PackageMatcher;

public interface OnlyBeAccessedSpecification<CONJUNCTION> {
    /**
     * Matches classes residing in a package matching any of the supplied package identifiers.
     *
     * @param packageIdentifiers Strings identifying packages, for details see {@link PackageMatcher}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION byAnyPackage(String... packageIdentifiers);
}
