/*
 * Copyright 2014-2025 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.library.dependencies.syntax;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.properties.CanOverrideDescription;
import com.tngtech.archunit.lang.syntax.elements.GivenObjects;
import com.tngtech.archunit.library.dependencies.Slice;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public interface GivenNamedSlices extends GivenObjects<Slice>, CanOverrideDescription<GivenNamedSlices> {

    @PublicAPI(usage = ACCESS)
    SlicesShould should();

    @Override
    @PublicAPI(usage = ACCESS)
    GivenNamedSlices as(String newDescription);

    @Override
    @PublicAPI(usage = ACCESS)
    GivenSlicesConjunction that(DescribedPredicate<? super Slice> predicate);
}
