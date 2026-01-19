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
package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.JavaMethod;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public interface MethodsThat<CONJUNCTION> extends CodeUnitsThat<CONJUNCTION> {

    /**
     * Matches static methods.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areStatic();

    /**
     * Matches non-static methods.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotStatic();

    /**
     * Matches final methods.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areFinal();

    /**
     * Matches non-final methods.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotFinal();

    /**
     * Matches methods that override other methods.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     * @see JavaMethod#isOverriding()
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areOverriding();

    /**
     * Matches methods that do not override other methods.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     * @see JavaMethod#isOverriding()
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotOverriding();
}
