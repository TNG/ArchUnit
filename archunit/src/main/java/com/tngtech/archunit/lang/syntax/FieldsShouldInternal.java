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
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.archunit.lang.syntax.elements.FieldsShould;
import com.tngtech.archunit.lang.syntax.elements.FieldsShouldConjunction;

import static com.tngtech.archunit.lang.conditions.ArchConditions.not;

class FieldsShouldInternal
        extends AbstractMembersShouldInternal<JavaField, FieldsShouldInternal>
        implements FieldsShould<FieldsShouldInternal>, FieldsShouldConjunction {

    FieldsShouldInternal(
            ClassesTransformer<? extends JavaField> classesTransformer,
            Priority priority,
            Function<ArchCondition<JavaField>, ArchCondition<JavaField>> prepareCondition) {

        super(classesTransformer, priority, prepareCondition);
    }

    FieldsShouldInternal(
            ClassesTransformer<? extends JavaField> classesTransformer,
            Priority priority,
            ArchCondition<JavaField> condition,
            Function<ArchCondition<JavaField>, ArchCondition<JavaField>> prepareCondition) {

        super(classesTransformer, priority, condition, prepareCondition);
    }

    private FieldsShouldInternal(
            ClassesTransformer<? extends JavaField> classesTransformer,
            Priority priority,
            ConditionAggregator<JavaField> conditionAggregator,
            Function<ArchCondition<JavaField>, ArchCondition<JavaField>> prepareCondition) {

        super(classesTransformer, priority, conditionAggregator, prepareCondition);
    }

    @Override
    FieldsShouldInternal copyWithNewCondition(ConditionAggregator<JavaField> newCondition) {
        return new FieldsShouldInternal(classesTransformer, priority, newCondition, prepareCondition);
    }

    @Override
    public FieldsShouldInternal haveRawType(Class<?> type) {
        return addCondition(ArchConditions.haveRawType(type));
    }

    @Override
    public FieldsShouldInternal notHaveRawType(Class<?> type) {
        return addCondition(not(ArchConditions.haveRawType(type)));
    }

    @Override
    public FieldsShouldInternal haveRawType(String typeName) {
        return addCondition(ArchConditions.haveRawType(typeName));
    }

    @Override
    public FieldsShouldInternal notHaveRawType(String typeName) {
        return addCondition(not(ArchConditions.haveRawType(typeName)));
    }

    @Override
    public FieldsShouldInternal haveRawType(DescribedPredicate<? super JavaClass> predicate) {
        return addCondition(ArchConditions.haveRawType(predicate));
    }

    @Override
    public FieldsShouldInternal notHaveRawType(DescribedPredicate<? super JavaClass> predicate) {
        return addCondition(not(ArchConditions.haveRawType(predicate)));
    }
}
