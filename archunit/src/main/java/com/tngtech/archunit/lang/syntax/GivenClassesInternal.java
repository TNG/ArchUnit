/*
 * Copyright 2014-2023 TNG Technology Consulting GmbH
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

import java.util.Optional;
import java.util.function.Function;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.elements.ClassesShould;
import com.tngtech.archunit.lang.syntax.elements.ClassesShouldConjunction;
import com.tngtech.archunit.lang.syntax.elements.ClassesThat;
import com.tngtech.archunit.lang.syntax.elements.GivenClasses;
import com.tngtech.archunit.lang.syntax.elements.GivenClassesConjunction;

class GivenClassesInternal extends AbstractGivenObjects<JavaClass, GivenClassesInternal>
        implements GivenClasses, GivenClassesConjunction {

    GivenClassesInternal(Priority priority, ClassesTransformer<JavaClass> classesTransformer) {
        this(priority, classesTransformer, Function.identity());
    }

    GivenClassesInternal(Priority priority, ClassesTransformer<JavaClass> classesTransformer,
            Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>> prepareCondition) {
        this(priority, classesTransformer, prepareCondition, new PredicateAggregator<>(), Optional.empty());
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
    public ClassesThat<GivenClassesConjunction> and() {
        return new ClassesThatInternal<>(input -> with(currentPredicate().thatANDs().add(input)));
    }

    @Override
    public ClassesThat<GivenClassesConjunction> or() {
        return new ClassesThatInternal<>(input -> with(currentPredicate().thatORs().add(input)));
    }

    @Override
    public ClassesThat<GivenClassesConjunction> that() {
        return new ClassesThatInternal<>(input -> with(currentPredicate().add(input)));
    }

    @Override
    public ClassesShouldConjunction should(ArchCondition<? super JavaClass> condition) {
        return new ClassesShouldInternal(finishedClassesTransformer(), priority, condition.forSubtype(), prepareCondition);
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
