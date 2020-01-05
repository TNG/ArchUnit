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

import java.util.List;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.archunit.lang.syntax.elements.CodeUnitsShould;
import com.tngtech.archunit.lang.syntax.elements.CodeUnitsShouldConjunction;

import static com.tngtech.archunit.lang.conditions.ArchConditions.not;

abstract class AbstractCodeUnitsShouldInternal<CODE_UNIT extends JavaCodeUnit, SELF extends AbstractCodeUnitsShouldInternal<CODE_UNIT, SELF>>
        extends AbstractMembersShouldInternal<CODE_UNIT, SELF>
        implements CodeUnitsShould<SELF>, CodeUnitsShouldConjunction<CODE_UNIT> {

    AbstractCodeUnitsShouldInternal(
            ClassesTransformer<? extends CODE_UNIT> classesTransformer,
            Priority priority,
            Function<ArchCondition<CODE_UNIT>, ArchCondition<CODE_UNIT>> prepareCondition) {
        super(classesTransformer, priority, prepareCondition);
    }

    AbstractCodeUnitsShouldInternal(
            ClassesTransformer<? extends CODE_UNIT> classesTransformer,
            Priority priority,
            ArchCondition<CODE_UNIT> condition,
            Function<ArchCondition<CODE_UNIT>, ArchCondition<CODE_UNIT>> prepareCondition) {

        super(classesTransformer, priority, condition, prepareCondition);
    }

    AbstractCodeUnitsShouldInternal(
            ClassesTransformer<? extends CODE_UNIT> classesTransformer,
            Priority priority,
            ConditionAggregator<CODE_UNIT> conditionAggregator,
            Function<ArchCondition<CODE_UNIT>, ArchCondition<CODE_UNIT>> prepareCondition) {
        super(classesTransformer, priority, conditionAggregator, prepareCondition);
    }

    @Override
    public SELF haveRawParameterTypes(Class<?>... parameterTypes) {
        return addCondition(ArchConditions.haveRawParameterTypes(parameterTypes));
    }

    @Override
    public SELF notHaveRawParameterTypes(Class<?>... parameterTypes) {
        return addCondition(not(ArchConditions.haveRawParameterTypes(parameterTypes)));
    }

    @Override
    public SELF haveRawParameterTypes(String... parameterTypeNames) {
        return addCondition(ArchConditions.haveRawParameterTypes(parameterTypeNames));
    }

    @Override
    public SELF notHaveRawParameterTypes(String... parameterTypeNames) {
        return addCondition(not(ArchConditions.haveRawParameterTypes(parameterTypeNames)));
    }

    @Override
    public SELF haveRawParameterTypes(DescribedPredicate<? super List<JavaClass>> predicate) {
        return addCondition(ArchConditions.haveRawParameterTypes(predicate));
    }

    @Override
    public SELF notHaveRawParameterTypes(DescribedPredicate<? super List<JavaClass>> predicate) {
        return addCondition(not(ArchConditions.haveRawParameterTypes(predicate)));
    }

    @Override
    public SELF haveRawReturnType(Class<?> type) {
        return addCondition(ArchConditions.haveRawReturnType(type));
    }

    @Override
    public SELF notHaveRawReturnType(Class<?> type) {
        return addCondition(not(ArchConditions.haveRawReturnType(type)));
    }

    @Override
    public SELF haveRawReturnType(String typeName) {
        return addCondition(ArchConditions.haveRawReturnType(typeName));
    }

    @Override
    public SELF notHaveRawReturnType(String typeName) {
        return addCondition(not(ArchConditions.haveRawReturnType(typeName)));
    }

    @Override
    public SELF haveRawReturnType(DescribedPredicate<? super JavaClass> predicate) {
        return addCondition(ArchConditions.haveRawReturnType(predicate));
    }

    @Override
    public SELF notHaveRawReturnType(DescribedPredicate<? super JavaClass> predicate) {
        return addCondition(not(ArchConditions.haveRawReturnType(predicate)));
    }

    @Override
    public SELF declareThrowableOfType(Class<? extends Throwable> type) {
        return addCondition(ArchConditions.declareThrowableOfType(type));
    }

    @Override
    public SELF notDeclareThrowableOfType(Class<? extends Throwable> type) {
        return addCondition(not(ArchConditions.declareThrowableOfType(type)));
    }

    @Override
    public SELF declareThrowableOfType(String typeName) {
        return addCondition(ArchConditions.declareThrowableOfType(typeName));
    }

    @Override
    public SELF notDeclareThrowableOfType(String typeName) {
        return addCondition(not(ArchConditions.declareThrowableOfType(typeName)));
    }

    @Override
    public SELF declareThrowableOfType(DescribedPredicate<? super JavaClass> predicate) {
        return addCondition(ArchConditions.declareThrowableOfType(predicate));
    }

    @Override
    public SELF notDeclareThrowableOfType(DescribedPredicate<? super JavaClass> predicate) {
        return addCondition(not(ArchConditions.declareThrowableOfType(predicate)));
    }

    static class CodeUnitsShouldInternal extends AbstractCodeUnitsShouldInternal<JavaCodeUnit, CodeUnitsShouldInternal> {

        CodeUnitsShouldInternal(
                ClassesTransformer<? extends JavaCodeUnit> classesTransformer,
                Priority priority,
                Function<ArchCondition<JavaCodeUnit>, ArchCondition<JavaCodeUnit>> prepareCondition) {
            super(classesTransformer, priority, prepareCondition);
        }

        CodeUnitsShouldInternal(
                ClassesTransformer<? extends JavaCodeUnit> classesTransformer, Priority priority,
                ArchCondition<JavaCodeUnit> condition,
                Function<ArchCondition<JavaCodeUnit>, ArchCondition<JavaCodeUnit>> prepareCondition) {
            super(classesTransformer, priority, condition, prepareCondition);
        }

        CodeUnitsShouldInternal(ClassesTransformer<? extends JavaCodeUnit> classesTransformer, Priority priority,
                ConditionAggregator<JavaCodeUnit> conditionAggregator,
                Function<ArchCondition<JavaCodeUnit>, ArchCondition<JavaCodeUnit>> prepareCondition) {
            super(classesTransformer, priority, conditionAggregator, prepareCondition);
        }

        @Override
        CodeUnitsShouldInternal copyWithNewCondition(ConditionAggregator<JavaCodeUnit> newCondition) {
            return new CodeUnitsShouldInternal(classesTransformer, priority, newCondition, prepareCondition);
        }
    }
}
