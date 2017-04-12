package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.elements.GivenConjunction;
import com.tngtech.archunit.lang.syntax.elements.GivenObjects;

abstract class AbstractGivenObjects<T, SELF extends AbstractGivenObjects<T, SELF>>
        implements GivenObjects<T>, GivenConjunction<T>, HasPredicates<T, SELF> {

    private final Factory<T, SELF> factory;
    final Priority priority;
    private final ClassesTransformer<T> classesTransformer;
    final Function<ArchCondition<T>, ArchCondition<T>> prepareCondition;
    private final PredicateAggregator<T> relevantObjectsPredicates;
    private final Optional<String> overriddenDescription;

    AbstractGivenObjects(Factory<T, SELF> factory,
                         Priority priority,
                         ClassesTransformer<T> classesTransformer,
                         Function<ArchCondition<T>, ArchCondition<T>> prepareCondition,
                         PredicateAggregator<T> relevantObjectsPredicates,
                         Optional<String> overriddenDescription) {
        this.factory = factory;
        this.prepareCondition = prepareCondition;
        this.classesTransformer = classesTransformer;
        this.overriddenDescription = overriddenDescription;
        this.priority = priority;
        this.relevantObjectsPredicates = relevantObjectsPredicates;
    }

    @Override
    public SELF with(DescribedPredicate<? super T> predicate) {
        PredicateAggregator<T> relevantObjectsPredicates = this.relevantObjectsPredicates.and(predicate);
        return factory.create(priority, classesTransformer, prepareCondition, relevantObjectsPredicates, overriddenDescription);
    }

    ClassesTransformer<T> finishedClassesTransformer() {
        ClassesTransformer<T> completeTransformation = relevantObjectsPredicates.isPresent() ?
                classesTransformer.that(relevantObjectsPredicates.get()) :
                classesTransformer;
        return overriddenDescription.isPresent() ?
                completeTransformation.as(overriddenDescription.get()) :
                completeTransformation;
    }

    @Override
    public SELF that(DescribedPredicate<? super T> predicate) {
        return with(predicate);
    }

    @Override
    public SELF and(DescribedPredicate<? super T> predicate) {
        return with(predicate);
    }

    interface Factory<T, GIVEN extends AbstractGivenObjects<T, GIVEN>> {
        GIVEN create(Priority priority,
                     ClassesTransformer<T> classesTransformer,
                     Function<ArchCondition<T>, ArchCondition<T>> prepareCondition,
                     PredicateAggregator<T> relevantObjectsPredicates,
                     Optional<String> overriddenDescription);
    }
}
