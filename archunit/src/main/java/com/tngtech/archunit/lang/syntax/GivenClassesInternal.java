/*
 * Copyright 2019 TNG Technology Consulting GmbH
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
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.elements.ClassesShould;
import com.tngtech.archunit.lang.syntax.elements.ClassesShouldConjunction;
import com.tngtech.archunit.lang.syntax.elements.GivenClasses;
import com.tngtech.archunit.lang.syntax.elements.GivenClassesConjunction;
import com.tngtech.archunit.lang.syntax.elements.GivenClassesThat;

class GivenClassesInternal extends AbstractGivenObjects<JavaClass, GivenClassesInternal>
        implements GivenClasses, GivenClassesConjunction {

    GivenClassesInternal(Priority priority, ClassesTransformer<JavaClass> classesTransformer) {
        this(priority, classesTransformer, Functions.<ArchCondition<JavaClass>>identity());
    }

    GivenClassesInternal(Priority priority, ClassesTransformer<JavaClass> classesTransformer,
            Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>> prepareCondition) {
        this(priority, classesTransformer, prepareCondition, new PredicateAggregator<JavaClass>(), Optional.<String>absent());
    }

    private GivenClassesInternal(
            Priority priority,
            ClassesTransformer<JavaClass> classesTransformer,
            Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>> prepareCondition,
            PredicateAggregator<JavaClass> relevantObjectsPredicates,
            Optional<String> overriddenDescription) {

        super(new GivenClassesFactory(),
                priority, classesTransformer, prepareCondition, relevantObjectsPredicates, overriddenDescription);

    }

    @Override
    public ClassesShould should() {
        return new ClassesShouldInternal(finishedClassesTransformer(), priority, prepareCondition);
    }

    @Override
    public GivenClassesThat and() {
        return new GivenClassesThatInternal(this, currentPredicate().thatANDs());
    }

    @Override
    public GivenClassesThat or() {
        return new GivenClassesThatInternal(this, currentPredicate().thatORs());
    }

    @Override
    public GivenClassesThat that() {
        return new GivenClassesThatInternal(this, currentPredicate());
    }

    @Override
    public ClassesShouldConjunction should(ArchCondition<? super JavaClass> condition) {
        return new ClassesShouldInternal(finishedClassesTransformer(), priority, condition.<JavaClass>forSubType(), prepareCondition);
    }

    private static class GivenClassesFactory implements Factory<JavaClass, GivenClassesInternal> {
        @Override
        public GivenClassesInternal create(Priority priority,
                ClassesTransformer<JavaClass> classesTransformer,
                Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>> prepareCondition,
                PredicateAggregator<JavaClass> relevantObjectsPredicates,
                Optional<String> overriddenDescription) {
            return new GivenClassesInternal(
                    priority, classesTransformer, prepareCondition, relevantObjectsPredicates, overriddenDescription);
        }
    }
}
