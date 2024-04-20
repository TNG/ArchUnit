/*
 * Copyright 2014-2024 TNG Technology Consulting GmbH
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
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.syntax.elements.MethodsThat;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaMethod.Predicates.overriding;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;

class MethodsThatInternal
        extends CodeUnitsThatInternal<JavaMethod, GivenMethodsInternal>
        implements MethodsThat<GivenMethodsInternal> {

    MethodsThatInternal(GivenMethodsInternal givenMethods, PredicateAggregator<JavaMethod> currentPredicate) {
        super(givenMethods, currentPredicate);
    }

    @Override
    public GivenMethodsInternal areOverriding() {
        return withPredicate(are(overriding()));
    }

    @Override
    public GivenMethodsInternal areNotOverriding() {
        return withPredicate(are(not(overriding())));
    }

    private GivenMethodsInternal withPredicate(DescribedPredicate<JavaMethod> predicate) {
        return givenMembers.with(currentPredicate.add(predicate));
    }
}
