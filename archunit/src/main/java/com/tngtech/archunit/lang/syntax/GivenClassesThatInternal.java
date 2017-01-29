package com.tngtech.archunit.lang.syntax;

import java.lang.annotation.Annotation;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.JavaAnnotation;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaModifier;
import com.tngtech.archunit.lang.syntax.elements.GivenClassesConjunction;
import com.tngtech.archunit.lang.syntax.elements.GivenClassesThat;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.core.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;

class GivenClassesThatInternal implements GivenClassesThat {
    private final GivenClassesInternal givenClasses;
    private final PredicateAggregator<JavaClass> currentPredicate;

    GivenClassesThatInternal(GivenClassesInternal givenClasses) {
        this(givenClasses, new PredicateAggregator<JavaClass>());
    }

    private GivenClassesThatInternal(GivenClassesInternal givenClasses, PredicateAggregator<JavaClass> predicate) {
        this.givenClasses = givenClasses;
        this.currentPredicate = predicate;
    }

    @Override
    public GivenClassesConjunction resideInPackage(String packageIdentifier) {
        return givenWith(JavaClass.Predicates.resideInPackage(packageIdentifier));
    }

    @Override
    public GivenClassesConjunction resideInAnyPackage(String... packageIdentifiers) {
        return givenWith(JavaClass.Predicates.resideInAnyPackage(packageIdentifiers));
    }

    @Override
    public GivenClassesConjunction resideOutsideOfPackage(String packageIdentifier) {
        return givenWith(ClassesThatPredicates.resideOutsideOfPackage(packageIdentifier));
    }

    @Override
    public GivenClassesConjunction resideOutsideOfPackages(String... packageIdentifiers) {
        return givenWith(ClassesThatPredicates.resideOutsideOfPackages(packageIdentifiers));
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
    public GivenClassesConjunction haveNameMatching(String regex) {
        return givenWith(have(nameMatching(regex)));
    }

    @Override
    public GivenClassesConjunction haveNameNotMatching(String regex) {
        return givenWith(ClassesThatPredicates.haveNameNotMatching(regex));
    }

    @Override
    public GivenClassesConjunction areAssignableTo(Class<?> type) {
        return givenWith(are(JavaClass.Predicates.assignableTo(type)));
    }

    @Override
    public GivenClassesConjunction areNotAssignableTo(Class<?> type) {
        return givenWith(are(not(JavaClass.Predicates.assignableTo(type))));
    }

    @Override
    public GivenClassesConjunction areAssignableTo(String typeName) {
        return givenWith(are(JavaClass.Predicates.assignableTo(typeName)));
    }

    @Override
    public GivenClassesConjunction areNotAssignableTo(String typeName) {
        return givenWith(are(not(JavaClass.Predicates.assignableTo(typeName))));
    }

    @Override
    public GivenClassesConjunction areAssignableTo(DescribedPredicate<? super JavaClass> predicate) {
        return givenWith(are(JavaClass.Predicates.assignableTo(predicate)));
    }

    @Override
    public GivenClassesConjunction areNotAssignableTo(DescribedPredicate<? super JavaClass> predicate) {
        return givenWith(are(not(JavaClass.Predicates.assignableTo(predicate))));
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
        return givenClasses.with(currentPredicate.and(predicate).get());
    }
}
