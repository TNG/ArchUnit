package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.elements.GivenObjects;

abstract class GivenObjectsInternal<T, SELF extends GivenObjectsInternal<T, SELF>> implements GivenObjects<T>, HasPredicates<T, SELF> {
    final Priority priority;
    final ClassesTransformer<T> classesTransformer;
    final Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>> prepareCondition;
    final PredicateAggregator<T> relevantObjectsPredicates;

    GivenObjectsInternal(Priority priority, ClassesTransformer<T> classesTransformer, Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>> prepareCondition) {
        this(priority, classesTransformer, prepareCondition, new PredicateAggregator<T>());
    }

    GivenObjectsInternal(
            Priority priority,
            ClassesTransformer<T> classesTransformer,
            Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>> prepareCondition,
            PredicateAggregator<T> relevantObjectsPredicates) {

        this.priority = priority;
        this.classesTransformer = classesTransformer;
        this.prepareCondition = prepareCondition;
        this.relevantObjectsPredicates = relevantObjectsPredicates;
    }
}
