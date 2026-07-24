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
package com.tngtech.archunit.lang;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Classifies the importance of an {@link EvaluationResult}, which is the result of {@link ArchRule#evaluate(JavaClasses)}.
 * <p>
 * {@link ArchRuleDefinition#priority(Priority)} can be used to create {@link ArchRule} implementations with a given priority.
 * </p>
 */
@PublicAPI(usage = ACCESS)
public enum Priority {
    @PublicAPI(usage = ACCESS)
    HIGH,
    @PublicAPI(usage = ACCESS)
    MEDIUM,
    @PublicAPI(usage = ACCESS)
    LOW;

    @PublicAPI(usage = ACCESS)
    public String asString() {
        return name();
    }
}
