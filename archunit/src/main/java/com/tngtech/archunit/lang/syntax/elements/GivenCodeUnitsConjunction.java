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
package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.lang.ArchCondition;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface GivenCodeUnitsConjunction<CODE_UNIT extends JavaCodeUnit> extends GivenMembersConjunction<CODE_UNIT> {

    @Override
    @PublicAPI(usage = ACCESS)
    CodeUnitsThat<? extends GivenCodeUnitsConjunction<CODE_UNIT>> and();

    @Override
    @PublicAPI(usage = ACCESS)
    CodeUnitsThat<? extends GivenCodeUnitsConjunction<CODE_UNIT>> or();

    @Override
    @PublicAPI(usage = ACCESS)
    GivenCodeUnitsConjunction<CODE_UNIT> and(DescribedPredicate<? super CODE_UNIT> predicate);

    @Override
    @PublicAPI(usage = ACCESS)
    GivenCodeUnitsConjunction<CODE_UNIT> or(DescribedPredicate<? super CODE_UNIT> predicate);

    @Override
    @PublicAPI(usage = ACCESS)
    CodeUnitsShouldConjunction<CODE_UNIT> should(ArchCondition<? super CODE_UNIT> condition);

    @Override
    @PublicAPI(usage = ACCESS)
    CodeUnitsShould<? extends CodeUnitsShouldConjunction<CODE_UNIT>> should();
}
