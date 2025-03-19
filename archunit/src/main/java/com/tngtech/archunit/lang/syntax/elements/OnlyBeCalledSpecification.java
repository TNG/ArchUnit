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
package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.properties.HasName;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public interface OnlyBeCalledSpecification<CONJUNCTION> {

    /**
     * Restricts allowed origins of calls to classes matching the supplied {@link DescribedPredicate}.
     * <br><br>
     * Note that many predefined {@link DescribedPredicate predicates} can be found within a subclass {@code Predicates} of the
     * respective domain object or a common ancestor. For example, {@link DescribedPredicate predicates} targeting
     * {@link JavaClass} can be found within {@link JavaClass.Predicates} or one of the respective ancestors like {@link HasName.Predicates}.
     *
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
     * Restricts allowed origins of calls to code units matching the supplied {@link DescribedPredicate}.
     * <br><br>
     * Note that many predefined {@link DescribedPredicate predicates} can be found within a subclass {@code Predicates} of the
     * respective domain object or a common ancestor. For example, {@link DescribedPredicate predicates} targeting
     * {@link JavaCodeUnit} can be found within {@link JavaCodeUnit.Predicates} or one of the respective ancestors
     * like {@link JavaMember.Predicates}.
     *
     * @param predicate Restricts which code units the call should originate from
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION byCodeUnitsThat(DescribedPredicate<? super JavaCodeUnit> predicate);

    /**
     * Restricts allowed origins of calls to methods matching the supplied {@link DescribedPredicate}.
     * <br><br>
     * Note that many predefined {@link DescribedPredicate predicates} can be found within a subclass {@code Predicates} of the
     * respective domain object or a common ancestor. For example, {@link DescribedPredicate predicates} targeting
     * {@link JavaMethod} can be found within {@link JavaMethod.Predicates} or one of the respective ancestors
     * like {@link JavaMember.Predicates}.
     *
     * @param predicate Restricts which methods the call should originate from. Calls from constructors are treated as mismatch.
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION byMethodsThat(DescribedPredicate<? super JavaMethod> predicate);

    /**
     * Restricts allowed origins of calls to constructors matching the supplied {@link DescribedPredicate}.
     * <br><br>
     * Note that many predefined {@link DescribedPredicate predicates} can be found within a subclass {@code Predicates} of the
     * respective domain object or a common ancestor. For example, {@link DescribedPredicate predicates} targeting
     * {@link JavaConstructor} can be found within {@link JavaConstructor.Predicates} or one of the respective ancestors
     * like {@link JavaMember.Predicates}.
     *
     * @param predicate Restricts which constructors the call should originate from. Calls from methods are treated as mismatch.
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION byConstructorsThat(DescribedPredicate<? super JavaConstructor> predicate);
}
