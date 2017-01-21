package com.tngtech.archunit.lang.syntax;

import java.lang.annotation.Annotation;

import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.lang.syntax.elements.GivenClassesConjunction;
import com.tngtech.archunit.lang.syntax.elements.GivenClassesThat;

import static com.tngtech.archunit.core.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;

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
        return givenClasses.with(currentPredicate.and(JavaClass.Predicates.resideInPackage(packageIdentifier)).get());
    }

    @Override
    public GivenClassesConjunction areAnnotatedWith(Class<? extends Annotation> annotationType) {
        return givenClasses.with(currentPredicate.and(are(annotatedWith(annotationType))).get());
    }
}
