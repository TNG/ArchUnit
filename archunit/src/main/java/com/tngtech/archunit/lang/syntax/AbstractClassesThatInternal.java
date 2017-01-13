package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.lang.syntax.elements.ClassesThat;

abstract class AbstractClassesThatInternal<T extends HasPredicates<JavaClass, T>, SELF extends ClassesThat<SELF>>
        implements ClassesThat<SELF> {
    final T givenClasses;
    final PredicateAggregator<JavaClass> currentPredicate;

    AbstractClassesThatInternal(T givenClasses) {
        this(givenClasses, new PredicateAggregator<JavaClass>());
    }

    AbstractClassesThatInternal(T givenClasses, PredicateAggregator<JavaClass> predicate) {
        this.givenClasses = givenClasses;
        this.currentPredicate = predicate;
    }

    @Override
    public SELF resideInPackage(String packageIdentifier) {
        return newSelf(currentPredicate.and(JavaClass.Predicates.resideInPackage(packageIdentifier)));
    }

    abstract SELF newSelf(PredicateAggregator<JavaClass> predicate);
}
