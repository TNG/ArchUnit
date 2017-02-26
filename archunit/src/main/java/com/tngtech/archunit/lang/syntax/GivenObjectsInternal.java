package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.Function.Functions;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.elements.GivenObjects;

public class GivenObjectsInternal<T> extends AbstractGivenObjects<T, GivenObjectsInternal<T>>
        implements GivenObjects<T>, HasPredicates<T, GivenObjectsInternal<T>> {

    GivenObjectsInternal(Priority priority, ClassesTransformer<T> classesTransformer) {
        this(priority, classesTransformer, Functions.<ArchCondition<T>>identity());
    }

    GivenObjectsInternal(Priority priority,
                         ClassesTransformer<T> classesTransformer,
                         Function<ArchCondition<T>, ArchCondition<T>> prepareCondition) {
        this(priority, classesTransformer, prepareCondition, new PredicateAggregator<T>(), Optional.<String>absent());
    }

    private GivenObjectsInternal(
            Priority priority,
            ClassesTransformer<T> classesTransformer,
            Function<ArchCondition<T>, ArchCondition<T>> prepareCondition,
            PredicateAggregator<T> relevantObjectsPredicates,
            Optional<String> overriddenDescription) {

        super(new GivenObjectsFactory<T>(),
                priority, classesTransformer, prepareCondition, relevantObjectsPredicates, overriddenDescription);
    }

    @Override
    public ArchRule should(ArchCondition<T> condition) {
        return new ObjectsShouldInternal<>(finishedClassesTransformer(), priority, condition, prepareCondition);
    }

    private static class GivenObjectsFactory<T> implements AbstractGivenObjects.Factory<T, GivenObjectsInternal<T>> {
        @Override
        public GivenObjectsInternal<T> create(Priority priority,
                                              ClassesTransformer<T> classesTransformer,
                                              Function<ArchCondition<T>, ArchCondition<T>> prepareCondition,
                                              PredicateAggregator<T> relevantObjectsPredicates,
                                              Optional<String> overriddenDescription) {

            return new GivenObjectsInternal<>(
                    priority, classesTransformer, prepareCondition, relevantObjectsPredicates, overriddenDescription);
        }
    }
}
