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
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.properties.HasThrowsClause;
import com.tngtech.archunit.lang.syntax.elements.CodeUnitsThat;

import static com.tngtech.archunit.base.DescribedPredicate.doNot;
import static com.tngtech.archunit.core.domain.properties.HasParameterTypes.Predicates.rawParameterTypes;
import static com.tngtech.archunit.core.domain.properties.HasReturnType.Predicates.rawReturnType;
import static com.tngtech.archunit.core.domain.properties.HasThrowsClause.Predicates.throwsClauseContainingType;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;

class CodeUnitsThatInternal<
        CODE_UNIT extends JavaCodeUnit,
        CONJUNCTION extends AbstractGivenCodeUnitsInternal<CODE_UNIT, CONJUNCTION>
        >
        extends MembersThatInternal<CODE_UNIT, CONJUNCTION>
        implements CodeUnitsThat<CONJUNCTION> {

    CodeUnitsThatInternal(CONJUNCTION givenCodeUnits, PredicateAggregator<CODE_UNIT> currentPredicate) {
        super(givenCodeUnits, currentPredicate);
    }

    @Override
    public CONJUNCTION haveRawParameterTypes(Class<?>... parameterTypes) {
        return withPredicate(have(rawParameterTypes(parameterTypes)));
    }

    @Override
    public CONJUNCTION doNotHaveRawParameterTypes(Class<?>... parameterTypes) {
        return withPredicate(doNot(have(rawParameterTypes(parameterTypes))));
    }

    @Override
    public CONJUNCTION haveRawParameterTypes(String... parameterTypeNames) {
        return withPredicate(have(rawParameterTypes(parameterTypeNames)));
    }

    @Override
    public CONJUNCTION doNotHaveRawParameterTypes(String... parameterTypeNames) {
        return withPredicate(doNot(have(rawParameterTypes(parameterTypeNames))));
    }

    @Override
    public CONJUNCTION haveRawParameterTypes(DescribedPredicate<? super List<JavaClass>> predicate) {
        return withPredicate(have(rawParameterTypes(predicate)));
    }

    @Override
    public CONJUNCTION doNotHaveRawParameterTypes(DescribedPredicate<? super List<JavaClass>> predicate) {
        return withPredicate(doNot(have(rawParameterTypes(predicate))));
    }

    @Override
    public CONJUNCTION haveRawReturnType(Class<?> type) {
        return withPredicate(have(rawReturnType(type)));
    }

    @Override
    public CONJUNCTION doNotHaveRawReturnType(Class<?> type) {
        return withPredicate(doNot(have(rawReturnType(type))));
    }

    @Override
    public CONJUNCTION haveRawReturnType(String typeName) {
        return withPredicate(have(rawReturnType(typeName)));
    }

    @Override
    public CONJUNCTION doNotHaveRawReturnType(String typeName) {
        return withPredicate(doNot(have(rawReturnType(typeName))));
    }

    @Override
    public CONJUNCTION haveRawReturnType(DescribedPredicate<? super JavaClass> predicate) {
        return withPredicate(have(rawReturnType(predicate)));
    }

    @Override
    public CONJUNCTION doNotHaveRawReturnType(DescribedPredicate<? super JavaClass> predicate) {
        return withPredicate(doNot(have(rawReturnType(predicate))));
    }

    @Override
    public CONJUNCTION declareThrowableOfType(Class<? extends Throwable> type) {
        return withPredicate(declareThrowableOfTypePredicate(type));
    }

    private DescribedPredicate<HasThrowsClause<?>> declareThrowableOfTypePredicate(Class<? extends Throwable> type) {
        return throwsClauseContainingType(type).as("declare throwable of type " + type.getName());
    }

    @Override
    public CONJUNCTION doNotDeclareThrowableOfType(Class<? extends Throwable> type) {
        return withPredicate(doNot(declareThrowableOfTypePredicate(type)));
    }

    @Override
    public CONJUNCTION declareThrowableOfType(String typeName) {
        return withPredicate(declareThrowableOfTypePredicate(typeName));
    }

    private DescribedPredicate<HasThrowsClause<?>> declareThrowableOfTypePredicate(String typeName) {
        return throwsClauseContainingType(typeName).as("declare throwable of type " + typeName);
    }

    @Override
    public CONJUNCTION doNotDeclareThrowableOfType(String typeName) {
        return withPredicate(doNot(declareThrowableOfTypePredicate(typeName)));
    }

    @Override
    public CONJUNCTION declareThrowableOfType(DescribedPredicate<? super JavaClass> predicate) {
        return withPredicate(declareThrowableOfTypePredicate(predicate));
    }

    private DescribedPredicate<HasThrowsClause<?>> declareThrowableOfTypePredicate(DescribedPredicate<? super JavaClass> predicate) {
        return throwsClauseContainingType(predicate).as("declare throwable of type " + predicate.getDescription());
    }

    @Override
    public CONJUNCTION doNotDeclareThrowableOfType(DescribedPredicate<? super JavaClass> predicate) {
        return withPredicate(doNot(declareThrowableOfTypePredicate(predicate)));
    }

    private CONJUNCTION withPredicate(DescribedPredicate<? super CODE_UNIT> predicate) {
        return givenMembers.with(currentPredicate.add(predicate));
    }
}
