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
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.elements.GivenFields;
import com.tngtech.archunit.lang.syntax.elements.GivenFieldsConjunction;

class GivenFieldsInternal extends AbstractGivenMembersInternal<JavaField, GivenFieldsInternal> implements GivenFields, GivenFieldsConjunction {

    GivenFieldsInternal(Priority priority, ClassesTransformer<JavaField> classesTransformer) {
        super(new GivenFieldsFactory(),
                priority,
                classesTransformer,
                Functions.<ArchCondition<JavaField>>identity(),
                new PredicateAggregator<JavaField>(),
                Optional.<String>absent());
    }

    private GivenFieldsInternal(
            Factory<JavaField, GivenFieldsInternal> factory,
            Priority priority,
            ClassesTransformer<JavaField> classesTransformer,
            Function<ArchCondition<JavaField>, ArchCondition<JavaField>> prepareCondition,
            PredicateAggregator<JavaField> relevantObjectsPredicates,
            Optional<String> overriddenDescription) {

        super(factory, priority, classesTransformer, prepareCondition, relevantObjectsPredicates, overriddenDescription);
    }

    @Override
    public ArchRule should(ArchCondition<? super JavaField> condition) {
        return new FieldsShouldInternal(finishedClassesTransformer(), priority, condition.<JavaField>forSubType(), prepareCondition);
    }

    private static class GivenFieldsFactory implements Factory<JavaField, GivenFieldsInternal> {
        @Override
        public GivenFieldsInternal create(
                Priority priority,
                ClassesTransformer<JavaField> classesTransformer,
                Function<ArchCondition<JavaField>, ArchCondition<JavaField>> prepareCondition,
                PredicateAggregator<JavaField> relevantObjectsPredicates,
                Optional<String> overriddenDescription) {

            return new GivenFieldsInternal(this,
                    priority, classesTransformer, prepareCondition, relevantObjectsPredicates, overriddenDescription);
        }
    }
}
