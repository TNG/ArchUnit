/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
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
import com.tngtech.archunit.base.HasDescription;

import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

/**
 * Allows to customize violation messages and rule text of {@link ArchRule ArchRules}.
 */
@PublicAPI(usage = INHERITANCE)
public interface FailureDisplayFormat {

    /**
     * Formats the failure of an {@link ArchRule}, i.e. constructs the text to display
     * for this failing rule, given the rule itself, its detailing failure message and
     * the rule {@link Priority}.
     *
     * @param rule The rule that failed
     * @param failureMessages All the failure details
     * @param priority The priority of the rule
     * @return A text to display for this failing rule
     */
    String formatFailure(HasDescription rule, FailureMessages failureMessages, Priority priority);
}
