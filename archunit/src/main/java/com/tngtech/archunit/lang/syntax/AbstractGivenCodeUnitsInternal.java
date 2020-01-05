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
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.AbstractCodeUnitsShouldInternal.CodeUnitsShouldInternal;
import com.tngtech.archunit.lang.syntax.elements.GivenCodeUnits;
import com.tngtech.archunit.lang.syntax.elements.GivenCodeUnitsConjunction;

abstract class AbstractGivenCodeUnitsInternal<MEMBER extends JavaCodeUnit, SELF extends AbstractGivenCodeUnitsInternal<MEMBER, SELF>>
        extends AbstractGivenMembersInternal<MEMBER, SELF>
        implements GivenCodeUnits<MEMBER>, GivenCodeUnitsConjunction<MEMBER> {

    AbstractGivenCodeUnitsInternal(
            Factory<MEMBER, SELF> factory,
            Priority priority,
            ClassesTransformer<MEMBER> classesTransformer,
            Function<ArchCondition<MEMBER>, ArchCondition<MEMBER>> prepareCondition,
            PredicateAggregator<MEMBER> relevantObjectsPredicates,
            Optional<String> overriddenDescription) {

        super(factory, priority, classesTransformer, prepareCondition, relevantObjectsPredicates, overriddenDescription);
    }

    @Override
    public CodeUnitsThatInternal<MEMBER, SELF> that() {
        return new CodeUnitsThatInternal<>(self(), currentPredicate());
    }

    @Override
    public CodeUnitsThatInternal<MEMBER, SELF> and() {
        return new CodeUnitsThatInternal<>(self(), currentPredicate().thatANDs());
    }

    @Override
    public CodeUnitsThatInternal<MEMBER, SELF> or() {
        return new CodeUnitsThatInternal<>(self(), currentPredicate().thatORs());
    }

    static class GivenCodeUnitsInternal extends AbstractGivenCodeUnitsInternal<JavaCodeUnit, GivenCodeUnitsInternal> {

        GivenCodeUnitsInternal(Priority priority, ClassesTransformer<JavaCodeUnit> classesTransformer) {
            this(priority, classesTransformer, Functions.<ArchCondition<JavaCodeUnit>>identity());
        }

        GivenCodeUnitsInternal(
                Priority priority,
                ClassesTransformer<JavaCodeUnit> classesTransformer,
                Function<ArchCondition<JavaCodeUnit>, ArchCondition<JavaCodeUnit>> prepareCondition) {

            super(new GivenCodeUnitsFactory(),
                    priority,
                    classesTransformer,
                    prepareCondition,
                    new PredicateAggregator<JavaCodeUnit>(),
                    Optional.<String>absent());
        }

        private GivenCodeUnitsInternal(
                Factory<JavaCodeUnit, GivenCodeUnitsInternal> factory,
                Priority priority,
                ClassesTransformer<JavaCodeUnit> classesTransformer,
                Function<ArchCondition<JavaCodeUnit>, ArchCondition<JavaCodeUnit>> prepareCondition,
                PredicateAggregator<JavaCodeUnit> relevantObjectsPredicates,
                Optional<String> overriddenDescription) {

            super(factory, priority, classesTransformer, prepareCondition, relevantObjectsPredicates, overriddenDescription);
        }

        @Override
        public CodeUnitsShouldInternal should() {
            return new CodeUnitsShouldInternal(finishedClassesTransformer(), priority, prepareCondition);
        }

        @Override
        public CodeUnitsShouldInternal should(ArchCondition<? super JavaCodeUnit> condition) {
            return new CodeUnitsShouldInternal(finishedClassesTransformer(), priority, condition.<JavaCodeUnit>forSubType(), prepareCondition);
        }

        private static class GivenCodeUnitsFactory implements Factory<JavaCodeUnit, GivenCodeUnitsInternal> {
            @Override
            public GivenCodeUnitsInternal create(
                    Priority priority,
                    ClassesTransformer<JavaCodeUnit> classesTransformer,
                    Function<ArchCondition<JavaCodeUnit>, ArchCondition<JavaCodeUnit>> prepareCondition,
                    PredicateAggregator<JavaCodeUnit> relevantObjectsPredicates,
                    Optional<String> overriddenDescription) {

                return new GivenCodeUnitsInternal(this,
                        priority, classesTransformer, prepareCondition, relevantObjectsPredicates, overriddenDescription);
            }
        }
    }
}
