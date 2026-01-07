/*
 * Copyright 2014-2026 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.library.modules.syntax;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.lang.syntax.elements.GivenConjunction;
import com.tngtech.archunit.library.modules.ArchModule;

import static com.tngtech.archunit.PublicAPI.State.EXPERIMENTAL;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
public interface GivenModulesConjunction<DESCRIPTOR extends ArchModule.Descriptor> extends GivenConjunction<ArchModule<DESCRIPTOR>> {

    @Override
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    GivenModulesConjunction<DESCRIPTOR> and(DescribedPredicate<? super ArchModule<DESCRIPTOR>> predicate);

    @Override
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    GivenModulesConjunction<DESCRIPTOR> or(DescribedPredicate<? super ArchModule<DESCRIPTOR>> predicate);

    /**
     * @see GivenModules#should()
     */
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    ModulesShould<DESCRIPTOR> should();

    /**
     * @see GivenModules#as(String, Object...)
     */
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    GivenModulesConjunction<DESCRIPTOR> as(String description, Object... args);
}
