package com.tngtech.archunit.lang.syntax;

import java.lang.annotation.Annotation;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.syntax.elements.GivenClassesConjunction;
import com.tngtech.archunit.lang.syntax.elements.GivenClassesThat;

import static com.tngtech.archunit.base.DescribedPredicate.dont;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;
import static com.tngtech.archunit.lang.syntax.ClassesThatPredicates.implementPredicate;

class GivenClassesThatInternal implements GivenClassesThat {
    private final GivenClassesInternal givenClasses;
    private final PredicateAggregator<JavaClass> currentPredicate;

    GivenClassesThatInternal(GivenClassesInternal givenClasses, PredicateAggregator<JavaClass> predicate) {
        this.givenClasses = givenClasses;
        this.currentPredicate = predicate;
    }

    @Override
    public GivenClassesConjunction resideInAPackage(String packageIdentifier) {
        return givenWith(JavaClass.Predicates.resideInAPackage(packageIdentifier));
    }

    @Override
    public GivenClassesConjunction resideInAnyPackage(String... packageIdentifiers) {
        return givenWith(JavaClass.Predicates.resideInAnyPackage(packageIdentifiers));
    }

    @Override
    public GivenClassesConjunction resideOutsideOfPackage(String packageIdentifier) {
        return givenWith(JavaClass.Predicates.resideOutsideOfPackage(packageIdentifier));
    }

    @Override
    public GivenClassesConjunction resideOutsideOfPackages(String... packageIdentifiers) {
        return givenWith(JavaClass.Predicates.resideOutsideOfPackages(packageIdentifiers));
    }

    @Override
    public GivenClassesConjunction areAnnotatedWith(Class<? extends Annotation> annotationType) {
        return givenWith(are(annotatedWith(annotationType)));
    }

    @Override
    public GivenClassesConjunction areNotAnnotatedWith(Class<? extends Annotation> annotationType) {
        return givenWith(are(not(annotatedWith(annotationType))));
    }

    @Override
    public GivenClassesConjunction areAnnotatedWith(String annotationTypeName) {
        return givenWith(are(annotatedWith(annotationTypeName)));
    }

    @Override
    public GivenClassesConjunction areNotAnnotatedWith(String annotationTypeName) {
        return givenWith(are(not(annotatedWith(annotationTypeName))));
    }

    @Override
    public GivenClassesConjunction areAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return givenWith(are(annotatedWith(predicate)));
    }

    @Override
    public GivenClassesConjunction areNotAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return givenWith(are(not(annotatedWith(predicate))));
    }

    @Override
    public GivenClassesConjunction implement(Class<?> type) {
        return givenWith(implementPredicate(assignableTo(type)));
    }

    @Override
    public GivenClassesConjunction dontImplement(Class<?> type) {
        return givenWith(dont(implementPredicate(assignableTo(type))));
    }

    @Override
    public GivenClassesConjunction implement(String typeName) {
        return givenWith(implementPredicate(assignableTo(typeName)));
    }

    @Override
    public GivenClassesConjunction dontImplement(String typeName) {
        return givenWith(dont(implementPredicate(assignableTo(typeName))));
    }

    @Override
    public GivenClassesConjunction implement(DescribedPredicate<? super JavaClass> predicate) {
        return givenWith(implementPredicate(assignableTo(predicate)));
    }

    @Override
    public GivenClassesConjunction dontImplement(DescribedPredicate<? super JavaClass> predicate) {
        return givenWith(dont(implementPredicate(assignableTo(predicate))));
    }

    @Override
    public GivenClassesConjunction haveNameMatching(String regex) {
        return givenWith(have(nameMatching(regex)));
    }

    @Override
    public GivenClassesConjunction haveNameNotMatching(String regex) {
        return givenWith(ClassesThatPredicates.haveNameNotMatching(regex));
    }

    @Override
    public GivenClassesConjunction areAssignableTo(Class<?> type) {
        return givenWith(are(assignableTo(type)));
    }

    @Override
    public GivenClassesConjunction areNotAssignableTo(Class<?> type) {
        return givenWith(are(not(assignableTo(type))));
    }

    @Override
    public GivenClassesConjunction areAssignableTo(String typeName) {
        return givenWith(are(assignableTo(typeName)));
    }

    @Override
    public GivenClassesConjunction areNotAssignableTo(String typeName) {
        return givenWith(are(not(assignableTo(typeName))));
    }

    @Override
    public GivenClassesConjunction areAssignableTo(DescribedPredicate<? super JavaClass> predicate) {
        return givenWith(are(assignableTo(predicate)));
    }

    @Override
    public GivenClassesConjunction areNotAssignableTo(DescribedPredicate<? super JavaClass> predicate) {
        return givenWith(are(not(assignableTo(predicate))));
    }

    @Override
    public GivenClassesConjunction areAssignableFrom(Class<?> type) {
        return givenWith(are(JavaClass.Predicates.assignableFrom(type)));
    }

    @Override
    public GivenClassesConjunction areNotAssignableFrom(Class<?> type) {
        return givenWith(are(not(JavaClass.Predicates.assignableFrom(type))));
    }

    @Override
    public GivenClassesConjunction areAssignableFrom(String typeName) {
        return givenWith(are(JavaClass.Predicates.assignableFrom(typeName)));
    }

    @Override
    public GivenClassesConjunction areNotAssignableFrom(String typeName) {
        return givenWith(are(not(JavaClass.Predicates.assignableFrom(typeName))));
    }

    @Override
    public GivenClassesConjunction areAssignableFrom(DescribedPredicate<? super JavaClass> predicate) {
        return givenWith(are(JavaClass.Predicates.assignableFrom(predicate)));
    }

    @Override
    public GivenClassesConjunction areNotAssignableFrom(DescribedPredicate<? super JavaClass> predicate) {
        return givenWith(are(not(JavaClass.Predicates.assignableFrom(predicate))));
    }

    @Override
    public GivenClassesConjunction arePublic() {
        return givenWith(ClassesThatPredicates.arePublic());
    }

    @Override
    public GivenClassesConjunction areNotPublic() {
        return givenWith(ClassesThatPredicates.areNotPublic());
    }

    @Override
    public GivenClassesConjunction areProtected() {
        return givenWith(ClassesThatPredicates.areProtected());
    }

    @Override
    public GivenClassesConjunction areNotProtected() {
        return givenWith(ClassesThatPredicates.areNotProtected());
    }

    @Override
    public GivenClassesConjunction arePackagePrivate() {
        return givenWith(ClassesThatPredicates.arePackagePrivate());
    }

    @Override
    public GivenClassesConjunction areNotPackagePrivate() {
        return givenWith(ClassesThatPredicates.areNotPackagePrivate());
    }

    @Override
    public GivenClassesConjunction arePrivate() {
        return givenWith(ClassesThatPredicates.arePrivate());
    }

    @Override
    public GivenClassesConjunction areNotPrivate() {
        return givenWith(ClassesThatPredicates.areNotPrivate());
    }

    @Override
    public GivenClassesConjunction areNamed(String name) {
        return givenWith(ClassesThatPredicates.areNamed(name));
    }

    @Override
    public GivenClassesConjunction areNotNamed(String name) {
        return givenWith(ClassesThatPredicates.areNotNamed(name));
    }

    @Override
    public GivenClassesConjunction haveSimpleName(String name) {
        return givenWith(ClassesThatPredicates.haveSimpleName(name));
    }

    @Override
    public GivenClassesConjunction dontHaveSimpleName(String name) {
        return givenWith(ClassesThatPredicates.dontHaveSimpleName(name));
    }

    @Override
    public GivenClassesConjunction haveModifier(JavaModifier modifier) {
        return givenWith(ClassesThatPredicates.haveModifier(modifier));
    }

    @Override
    public GivenClassesConjunction dontHaveModifier(JavaModifier modifier) {
        return givenWith(ClassesThatPredicates.dontHaveModifier(modifier));
    }

    private GivenClassesInternal givenWith(DescribedPredicate<? super JavaClass> predicate) {
        return givenClasses.with(currentPredicate.add(predicate));
    }
}
