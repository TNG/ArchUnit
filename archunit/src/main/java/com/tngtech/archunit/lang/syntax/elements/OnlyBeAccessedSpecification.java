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
package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.PackageMatcher;
import com.tngtech.archunit.core.domain.JavaClass;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface OnlyBeAccessedSpecification<CONJUNCTION> {
    /**
     * Matches classes residing in a package matching any of the supplied package identifiers.
     *
     * @param packageIdentifiers Strings identifying packages, for details see {@link PackageMatcher}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION byAnyPackage(String... packageIdentifiers);

    /**
     * @return A syntax element that allows restricting which classes the access should be from
     */
    @PublicAPI(usage = ACCESS)
    ClassesThat<ClassesShouldConjunction> byClassesThat();

    /**
     * @param predicate Restricts which classes the access should be from
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION byClassesThat(DescribedPredicate<? super JavaClass> predicate);
}
