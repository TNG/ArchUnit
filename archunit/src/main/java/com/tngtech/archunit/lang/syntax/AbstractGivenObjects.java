/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        implements GivenObjects<T>, GivenConjunction<T> {

    final Factory<T, SELF> factory;
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

    SELF with(PredicateAggregator<T> newPredicate) {
        return factory.create(priority, classesTransformer, prepareCondition, newPredicate, overriddenDescription);
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
        return with(currentPredicate().add(predicate));
    }

    @Override
    public SELF and(DescribedPredicate<? super T> predicate) {
        return with(currentPredicate().thatANDs().add(predicate));
    }

    @Override
    public SELF or(DescribedPredicate<? super T> predicate) {
        return with(currentPredicate().thatORs().add(predicate));
    }

    @SuppressWarnings("unchecked")
        // By convention SELF is always our own type
    SELF self() {
        return (SELF) this;
    }

    PredicateAggregator<T> currentPredicate() {
        return relevantObjectsPredicates;
    }

    interface Factory<T, GIVEN extends AbstractGivenObjects<T, GIVEN>> {
        GIVEN create(Priority priority,
                ClassesTransformer<T> classesTransformer,
                Function<ArchCondition<T>, ArchCondition<T>> prepareCondition,
                PredicateAggregator<T> relevantObjectsPredicates,
                Optional<String> overriddenDescription);
    }
}
