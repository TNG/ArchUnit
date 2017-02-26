package com.tngtech.archunit.lang.syntax.elements;

import java.lang.annotation.Annotation;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.JavaAnnotation;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaModifier;

public interface ClassesThat<CONJUNCTION> {

    CONJUNCTION areNamed(String name);

    CONJUNCTION areNotNamed(String name);

    CONJUNCTION haveSimpleName(String name);

    CONJUNCTION dontHaveSimpleName(String name);

    CONJUNCTION haveNameMatching(String regex);

    CONJUNCTION haveNameNotMatching(String regex);

    /**
     * @see com.tngtech.archunit.base.PackageMatcher
     */
    CONJUNCTION resideInPackage(String packageIdentifier);

    /**
     * @see com.tngtech.archunit.base.PackageMatcher
     */
    CONJUNCTION resideInAnyPackage(String... packageIdentifiers);

    /**
     * @see com.tngtech.archunit.base.PackageMatcher
     */
    CONJUNCTION resideOutsideOfPackage(String packageIdentifier);

    /**
     * @see com.tngtech.archunit.base.PackageMatcher
     */
    CONJUNCTION resideOutsideOfPackages(String... packageIdentifiers);

    CONJUNCTION arePublic();

    CONJUNCTION areNotPublic();

    CONJUNCTION areProtected();

    CONJUNCTION areNotProtected();

    CONJUNCTION arePackagePrivate();

    CONJUNCTION areNotPackagePrivate();

    CONJUNCTION arePrivate();

    CONJUNCTION areNotPrivate();

    CONJUNCTION haveModifier(JavaModifier modifier);

    CONJUNCTION dontHaveModifier(JavaModifier modifier);

    CONJUNCTION areAnnotatedWith(Class<? extends Annotation> annotationType);

    CONJUNCTION areNotAnnotatedWith(Class<? extends Annotation> annotationType);

    CONJUNCTION areAnnotatedWith(String annotationTypeName);

    CONJUNCTION areNotAnnotatedWith(String annotationTypeName);

    CONJUNCTION areAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate);

    CONJUNCTION areNotAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate);

    CONJUNCTION areAssignableTo(Class<?> type);

    CONJUNCTION areNotAssignableTo(Class<?> type);

    CONJUNCTION areAssignableTo(String typeName);

    CONJUNCTION areNotAssignableTo(String typeName);

    CONJUNCTION areAssignableTo(DescribedPredicate<? super JavaClass> predicate);

    CONJUNCTION areNotAssignableTo(DescribedPredicate<? super JavaClass> predicate);

    CONJUNCTION areAssignableFrom(Class<?> type);

    CONJUNCTION areNotAssignableFrom(Class<?> type);

    CONJUNCTION areAssignableFrom(String typeName);

    CONJUNCTION areNotAssignableFrom(String typeName);

    CONJUNCTION areAssignableFrom(DescribedPredicate<? super JavaClass> predicate);

    CONJUNCTION areNotAssignableFrom(DescribedPredicate<? super JavaClass> predicate);
}
