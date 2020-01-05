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
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.elements.MethodsShould;
import com.tngtech.archunit.lang.syntax.elements.MethodsShouldConjunction;

class MethodsShouldInternal
        extends AbstractCodeUnitsShouldInternal<JavaMethod, MethodsShouldInternal>
        implements MethodsShould<MethodsShouldInternal>, MethodsShouldConjunction {

    MethodsShouldInternal(
            ClassesTransformer<? extends JavaMethod> classesTransformer,
            Priority priority,
            Function<ArchCondition<JavaMethod>, ArchCondition<JavaMethod>> prepareCondition) {

        super(classesTransformer, priority, prepareCondition);
    }

    MethodsShouldInternal(
            ClassesTransformer<? extends JavaMethod> classesTransformer,
            Priority priority,
            ArchCondition<JavaMethod> condition,
            Function<ArchCondition<JavaMethod>, ArchCondition<JavaMethod>> prepareCondition) {

        super(classesTransformer, priority, condition, prepareCondition);
    }

    private MethodsShouldInternal(
            ClassesTransformer<? extends JavaMethod> classesTransformer,
            Priority priority,
            ConditionAggregator<JavaMethod> conditionAggregator,
            Function<ArchCondition<JavaMethod>, ArchCondition<JavaMethod>> prepareCondition) {

        super(classesTransformer, priority, conditionAggregator, prepareCondition);
    }

    @Override
    MethodsShouldInternal copyWithNewCondition(ConditionAggregator<JavaMethod> newCondition) {
        return new MethodsShouldInternal(classesTransformer, priority, newCondition, prepareCondition);
    }
}
