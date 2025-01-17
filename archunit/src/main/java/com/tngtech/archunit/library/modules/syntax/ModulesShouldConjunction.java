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
package com.tngtech.archunit.library.modules.syntax;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.modules.ArchModule;

import static com.tngtech.archunit.PublicAPI.State.EXPERIMENTAL;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
public interface ModulesShouldConjunction<DESCRIPTOR extends ArchModule.Descriptor> {

    /**
     * Like {@link #andShould(ArchCondition)} but offers a fluent API to pick the condition to join.
     */
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    ModulesShould<DESCRIPTOR> andShould();

    /**
     * Joins another condition to this rule with {@code and} semantics. That is, all modules under test
     * now needs to satisfy the existing condition and this new one.<br>
     * Note that this is always left-associative and does not support any operator precedence.
     *
     * @param condition Another condition to be 'and'-ed to the current condition of this rule
     * @return An {@link ArchRule} to check against imported {@link JavaClasses}
     */
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    ArchRule andShould(ArchCondition<? super ArchModule<DESCRIPTOR>> condition);
}
