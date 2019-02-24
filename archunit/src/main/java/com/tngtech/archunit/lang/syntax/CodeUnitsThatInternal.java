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

import java.util.List;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.lang.syntax.elements.CodeUnitsThat;

import static com.tngtech.archunit.core.domain.properties.HasParameterTypes.Predicates.rawParameterTypes;
import static com.tngtech.archunit.core.domain.properties.HasReturnType.Predicates.rawReturnType;
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
        return givenMembers.with(currentPredicate.add(have(rawParameterTypes(parameterTypes))));
    }

    @Override
    public CONJUNCTION haveRawParameterTypes(String... parameterTypeNames) {
        return givenMembers.with(currentPredicate.add(have(rawParameterTypes(parameterTypeNames))));
    }

    @Override
    public CONJUNCTION haveRawParameterTypes(DescribedPredicate<List<JavaClass>> predicate) {
        return givenMembers.with(currentPredicate.add(have(rawParameterTypes(predicate))));
    }

    @Override
    public CONJUNCTION haveRawReturnType(Class<?> type) {
        return givenMembers.with(currentPredicate.add(have(rawReturnType(type))));

    }

    @Override
    public CONJUNCTION haveRawReturnType(String typeName) {
        return givenMembers.with(currentPredicate.add(have(rawReturnType(typeName))));

    }

    @Override
    public CONJUNCTION haveRawReturnType(DescribedPredicate<JavaClass> predicate) {
        return givenMembers.with(currentPredicate.add(have(rawReturnType(predicate))));

    }
}
