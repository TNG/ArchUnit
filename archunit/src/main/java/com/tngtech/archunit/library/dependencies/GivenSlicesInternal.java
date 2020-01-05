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
package com.tngtech.archunit.library.dependencies;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.dependencies.syntax.GivenNamedSlices;
import com.tngtech.archunit.library.dependencies.syntax.GivenSlices;
import com.tngtech.archunit.library.dependencies.syntax.GivenSlicesConjunction;
import com.tngtech.archunit.library.dependencies.syntax.SlicesShould;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.Guava.Iterables.filter;

class GivenSlicesInternal implements GivenSlices, SlicesShould, GivenSlicesConjunction {
    private final Priority priority;
    private final Slices.Transformer classesTransformer;

    GivenSlicesInternal(Priority priority, Slices.Transformer classesTransformer) {
        this.classesTransformer = checkNotNull(classesTransformer);
        this.priority = checkNotNull(priority);
    }

    @Override
    public ArchRule should(ArchCondition<? super Slice> condition) {
        return ArchRule.Factory.create(classesTransformer, condition.<Slice>forSubType(), priority);
    }

    @Override
    public GivenSlicesInternal that(DescribedPredicate<? super Slice> predicate) {
        return givenWith(classesTransformer.that(predicate));
    }

    @Override
    public GivenSlicesInternal and(DescribedPredicate<? super Slice> predicate) {
        return givenWith(classesTransformer.thatANDsPredicates().that(predicate));
    }

    @Override
    public GivenSlicesInternal or(DescribedPredicate<? super Slice> predicate) {
        return givenWith(classesTransformer.thatORsPredicates().that(predicate));
    }

    private GivenSlicesInternal givenWith(Slices.Transformer transformer) {
        return new GivenSlicesInternal(priority, transformer);
    }

    @Override
    public GivenSlicesInternal as(String newDescription) {
        return givenWith(classesTransformer.as(newDescription));
    }

    /**
     * @see Slices#namingSlices(String)
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public GivenNamedSlices namingSlices(String pattern) {
        return new GivenSlicesInternal(priority, classesTransformer.namingSlices(pattern));
    }

    @Override
    public SlicesShould should() {
        return this;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public SliceRule beFreeOfCycles() {
        return new SliceRule(classesTransformer, priority, new SliceRule.ConditionFactory() {
            @Override
            public ArchCondition<Slice> create(Slices.Transformer transformer, DescribedPredicate<Dependency> predicate) {
                return new SliceCycleArchCondition(predicate);
            }
        });
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public SliceRule notDependOnEachOther() {
        return new SliceRule(classesTransformer, priority, new SliceRule.ConditionFactory() {
            @Override
            public ArchCondition<Slice> create(Slices.Transformer transformer, DescribedPredicate<Dependency> predicate) {
                return new NotDependOnEachOtherCondition(predicate, transformer);
            }
        });
    }

    private static class NotDependOnEachOtherCondition extends ArchCondition<Slice> {
        private final DescribedPredicate<Dependency> predicate;
        private final Slices.Transformer inputTransformer;

        NotDependOnEachOtherCondition(DescribedPredicate<Dependency> predicate, Slices.Transformer inputTransformer) {
            super("not depend on each other");
            this.predicate = predicate;
            this.inputTransformer = inputTransformer;
        }

        @Override
        public void check(Slice slice, ConditionEvents events) {
            Iterable<Dependency> relevantDependencies = filter(slice.getDependenciesFromSelf(), predicate);
            Slices dependencySlices = inputTransformer.transform(relevantDependencies);
            for (Slice dependencySlice : dependencySlices) {
                SliceDependency dependency = SliceDependency.of(slice, relevantDependencies, dependencySlice);
                events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
            }
        }
    }
}
