package com.tngtech.archunit.lang.syntax;

import java.lang.annotation.Annotation;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.JavaClass;
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
    public GivenClassesConjunction resideOutsideOfPackage(String packageIdentifier) {
        return givenWith(negate(JavaClass.Predicates.resideInPackage(packageIdentifier),
                "reside in", "reside outside of"));
    }

    static DescribedPredicate<JavaClass> negate(DescribedPredicate<JavaClass> predicate, String partToReplace, String replaceWith) {
        return not(predicate).as(predicate.getDescription().replace(partToReplace, replaceWith));
    }

    @Override
    public GivenClassesConjunction areAnnotatedWith(Class<? extends Annotation> annotationType) {
        return givenWith(are(annotatedWith(annotationType)));
    }

    @Override
    public GivenClassesConjunction haveNameMatching(String regex) {
        return givenWith(have(nameMatching(regex)));
    }

    @Override
    public GivenClassesConjunction areAssignableTo(Class<?> type) {
        return givenWith(are(JavaClass.Predicates.assignableTo(type)));
    }

    private GivenClassesInternal givenWith(DescribedPredicate<? super JavaClass> predicate) {
        return givenClasses.with(currentPredicate.and(predicate).get());
    }
}
