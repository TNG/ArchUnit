package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.lang.syntax.elements.ClassesShould;
import com.tngtech.archunit.lang.syntax.elements.GivenClassesThat;

class GivenClassesThatInternal extends AbstractClassesThatInternal<GivenClassesInternal, GivenClassesThat>
        implements GivenClassesThat {
    GivenClassesThatInternal(GivenClassesInternal givenClasses) {
        super(givenClasses);
    }

    private GivenClassesThatInternal(GivenClassesInternal givenClasses, PredicateAggregator<JavaClass> predicate) {
        super(givenClasses, predicate);
    }

    @Override
    GivenClassesThat newSelf(PredicateAggregator<JavaClass> predicate) {
        return new GivenClassesThatInternal(givenClasses, predicate);
    }

    @Override
    public ClassesShould should() {
        return currentPredicate.addTo(givenClasses).should();
    }
}
