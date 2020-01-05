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
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.elements.GivenConstructors;
import com.tngtech.archunit.lang.syntax.elements.GivenConstructorsConjunction;

class GivenConstructorsInternal extends AbstractGivenCodeUnitsInternal<JavaConstructor, GivenConstructorsInternal>
        implements GivenConstructors, GivenConstructorsConjunction {

    GivenConstructorsInternal(Priority priority, ClassesTransformer<JavaConstructor> classesTransformer) {
        this(priority, classesTransformer, Functions.<ArchCondition<JavaConstructor>>identity());
    }

    GivenConstructorsInternal(
            Priority priority,
            ClassesTransformer<JavaConstructor> classesTransformer,
            Function<ArchCondition<JavaConstructor>, ArchCondition<JavaConstructor>> prepareCondition) {

        this(new GivenConstructorsFactory(),
                priority,
                classesTransformer,
                prepareCondition,
                new PredicateAggregator<JavaConstructor>(),
                Optional.<String>absent());
    }

    private GivenConstructorsInternal(
            Factory<JavaConstructor, GivenConstructorsInternal> factory,
            Priority priority,
            ClassesTransformer<JavaConstructor> classesTransformer,
            Function<ArchCondition<JavaConstructor>, ArchCondition<JavaConstructor>> prepareCondition,
            PredicateAggregator<JavaConstructor> relevantObjectsPredicates,
            Optional<String> overriddenDescription) {

        super(factory, priority, classesTransformer, prepareCondition, relevantObjectsPredicates, overriddenDescription);
    }

    @Override
    public ConstructorsShouldInternal should() {
        return new ConstructorsShouldInternal(finishedClassesTransformer(), priority, prepareCondition);
    }

    @Override
    public ConstructorsShouldInternal should(ArchCondition<? super JavaConstructor> condition) {
        return new ConstructorsShouldInternal(finishedClassesTransformer(), priority, condition.<JavaConstructor>forSubType(), prepareCondition);
    }

    private static class GivenConstructorsFactory implements Factory<JavaConstructor, GivenConstructorsInternal> {
        @Override
        public GivenConstructorsInternal create(
                Priority priority,
                ClassesTransformer<JavaConstructor> classesTransformer,
                Function<ArchCondition<JavaConstructor>, ArchCondition<JavaConstructor>> prepareCondition,
                PredicateAggregator<JavaConstructor> relevantObjectsPredicates,
                Optional<String> overriddenDescription) {

            return new GivenConstructorsInternal(this,
                    priority, classesTransformer, prepareCondition, relevantObjectsPredicates, overriddenDescription);
        }
    }
}
