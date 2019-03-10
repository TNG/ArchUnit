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

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.lang.syntax.elements.FieldsThat;

import static com.tngtech.archunit.core.domain.properties.HasType.Predicates.type;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;

public class GivenFieldsThatInternal extends GivenMembersThatInternal<JavaField, GivenFieldsInternal>
        implements FieldsThat<GivenFieldsInternal> {

    GivenFieldsThatInternal(GivenFieldsInternal givenFields, PredicateAggregator<JavaField> currentPredicate) {
        super(givenFields, currentPredicate);
    }

    @Override
    public GivenFieldsInternal haveType(Class<?> type) {
        return givenMembers.with(currentPredicate.add(have(type(type))));
    }

    @Override
    public GivenFieldsInternal haveType(String typeName) {
        return givenMembers.with(currentPredicate.add(have(type(typeName))));
    }

    @Override
    public GivenFieldsInternal haveType(DescribedPredicate<? super JavaClass> predicate) {
        return givenMembers.with(currentPredicate.add(have(type(predicate))));
    }
}
