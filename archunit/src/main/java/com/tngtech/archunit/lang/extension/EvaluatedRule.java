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
package com.tngtech.archunit.lang.extension;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;

import static com.tngtech.archunit.PublicAPI.State.EXPERIMENTAL;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Bundles an {@link ArchRule} together with the {@link JavaClasses} that were evaluated, and the
 * respective {@link EvaluationResult}. To react to failures during evaluation of the rule,
 * see {@link EvaluationResult}.
 */
public interface EvaluatedRule {
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    ArchRule getRule();

    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    JavaClasses getClasses();

    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    EvaluationResult getResult();
}
