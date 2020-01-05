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
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.properties.HasType;
import com.tngtech.archunit.lang.syntax.elements.FieldsThat;

import static com.tngtech.archunit.base.DescribedPredicate.doNot;
import static com.tngtech.archunit.core.domain.properties.HasType.Predicates.rawType;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;

class FieldsThatInternal extends MembersThatInternal<JavaField, GivenFieldsInternal>
        implements FieldsThat<GivenFieldsInternal> {

    FieldsThatInternal(GivenFieldsInternal givenFields, PredicateAggregator<JavaField> currentPredicate) {
        super(givenFields, currentPredicate);
    }

    @Override
    public GivenFieldsInternal haveRawType(Class<?> type) {
        return with(have(rawType(type)));
    }

    @Override
    public GivenFieldsInternal doNotHaveRawType(Class<?> type) {
        return with(doNot(have(rawType(type))));
    }

    @Override
    public GivenFieldsInternal haveRawType(String typeName) {
        return with(have(rawType(typeName)));
    }

    @Override
    public GivenFieldsInternal doNotHaveRawType(String typeName) {
        return with(doNot(have(rawType(typeName))));
    }

    @Override
    public GivenFieldsInternal haveRawType(DescribedPredicate<? super JavaClass> predicate) {
        return with(have(rawType(predicate)));
    }

    @Override
    public GivenFieldsInternal doNotHaveRawType(DescribedPredicate<? super JavaClass> predicate) {
        return with(doNot(have(rawType(predicate))));
    }

    private GivenFieldsInternal with(DescribedPredicate<HasType> predicate) {
        return givenMembers.with(currentPredicate.add(predicate));
    }
}
