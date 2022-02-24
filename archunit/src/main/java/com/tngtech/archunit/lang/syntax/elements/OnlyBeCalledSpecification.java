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
package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaMember;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface OnlyBeCalledSpecification<CONJUNCTION> {

    /**
     * @param predicate Restricts which classes the call should originate from. Every class that calls the respective {@link JavaCodeUnit} must match the predicate.
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION byClassesThat(DescribedPredicate<? super JavaClass> predicate);

    /**
     * @return A syntax element that allows restricting which classes the call should be from
     */
    @PublicAPI(usage = ACCESS)
    ClassesThat<CONJUNCTION> byClassesThat();

    /**
     * @param predicate Restricts which code units the call should originate from
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION byCodeUnitsThat(DescribedPredicate<? super JavaMember> predicate);

    /**
     * @param predicate Restricts which methods the call should originate from. Calls from constructors are treated as mismatch.
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION byMethodsThat(DescribedPredicate<? super JavaMember> predicate);

    /**
     * @param predicate Restricts which constructors the call should originate from. Calls from methods are treated as mismatch.
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION byConstructorsThat(DescribedPredicate<? super JavaMember> predicate);
}
