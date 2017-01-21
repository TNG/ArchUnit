package com.tngtech.archunit.lang.syntax.elements;

import java.lang.annotation.Annotation;

public interface ClassesThat<CONJUNCTION extends Conjunction> {
    /**
     * @see com.tngtech.archunit.base.PackageMatcher
     */
    CONJUNCTION resideInPackage(String packageIdentifier);

    /**
     * @see com.tngtech.archunit.base.PackageMatcher
     */
    CONJUNCTION resideOutsideOfPackage(String packageIdentifier);

    CONJUNCTION areAnnotatedWith(Class<? extends Annotation> annotationType);

    CONJUNCTION haveNameMatching(String regex);

    CONJUNCTION areAssignableTo(Class<?> type);
}
