/*
 * Copyright 2018 TNG Technology Consulting GmbH
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
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.elements.GivenMethods;
import com.tngtech.archunit.lang.syntax.elements.GivenMethodsConjunction;

class GivenMethodsInternal extends AbstractGivenCodeUnitsInternal<JavaMethod, GivenMethodsInternal>
        implements GivenMethods, GivenMethodsConjunction {

    GivenMethodsInternal(Priority priority, ClassesTransformer<JavaMethod> classesTransformer) {
        this(new GivenMethodsFactory(),
                priority,
                classesTransformer,
                Functions.<ArchCondition<JavaMethod>>identity(),
                new PredicateAggregator<JavaMethod>(),
                Optional.<String>absent());
    }

    private GivenMethodsInternal(
            Factory<JavaMethod, GivenMethodsInternal> factory,
            Priority priority,
            ClassesTransformer<JavaMethod> classesTransformer,
            Function<ArchCondition<JavaMethod>, ArchCondition<JavaMethod>> prepareCondition,
            PredicateAggregator<JavaMethod> relevantObjectsPredicates,
            Optional<String> overriddenDescription) {

        super(factory, priority, classesTransformer, prepareCondition, relevantObjectsPredicates, overriddenDescription);
    }

    @Override
    public ArchRule should(ArchCondition<? super JavaMethod> condition) {
        return new MethodsShouldInternal(finishedClassesTransformer(), priority, condition.<JavaMethod>forSubType(), prepareCondition);
    }

    private static class GivenMethodsFactory implements Factory<JavaMethod, GivenMethodsInternal> {
        @Override
        public GivenMethodsInternal create(
                Priority priority,
                ClassesTransformer<JavaMethod> classesTransformer,
                Function<ArchCondition<JavaMethod>, ArchCondition<JavaMethod>> prepareCondition,
                PredicateAggregator<JavaMethod> relevantObjectsPredicates,
                Optional<String> overriddenDescription) {

            return new GivenMethodsInternal(this,
                    priority, classesTransformer, prepareCondition, relevantObjectsPredicates, overriddenDescription);
        }
    }
}
