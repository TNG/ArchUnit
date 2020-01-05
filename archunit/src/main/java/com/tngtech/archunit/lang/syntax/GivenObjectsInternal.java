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

import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.Function.Functions;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.elements.GivenObjects;

class GivenObjectsInternal<T> extends AbstractGivenObjects<T, GivenObjectsInternal<T>> implements GivenObjects<T> {

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
    public ArchRule should(ArchCondition<? super T> condition) {
        return new ObjectsShouldInternal<>(finishedClassesTransformer(), priority, condition.<T>forSubType(), prepareCondition);
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
