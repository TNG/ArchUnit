package com.tngtech.archunit.core.domain.packageexamples.second;

import com.tngtech.archunit.core.domain.packageexamples.first.First2;

public class Second1 {
    First2 first2;
    // Since we meanwhile consider arrays to be within the package of their component type, we will always run into the problem
    // that a class is within the same package but was not originally imported.
    // Those classes should nevertheless have the correct package attached in a consistent way
    ClassDependingOnOtherSecondClass[] evilArrayCausingClassInSamePackageNotOriginallyImported;
}
