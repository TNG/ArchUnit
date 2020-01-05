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
package com.tngtech.archunit.library.freeze;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

/**
 * Allows {@link FreezingArchRule} to decide when two lines of two violations count as "equivalent".
 */
@PublicAPI(usage = INHERITANCE)
public interface ViolationLineMatcher {

    /**
     * @param lineFromFirstViolation A line from the description of a violation of an {@link ArchRule} being evaluated
     * @param lineFromSecondViolation A line from the description of a stored violation of an {@link ArchRule}
     * @return true, if and only if those two lines should be considered equivalent
     */
    boolean matches(String lineFromFirstViolation, String lineFromSecondViolation);
}
