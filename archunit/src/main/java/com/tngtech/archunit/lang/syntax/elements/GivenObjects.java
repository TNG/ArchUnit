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
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface GivenObjects<T> {

    /**
     * Allows to form a rule by passing a condition the objects under consideration must satisfy. E.g.
     * <br><br>
     * <code>
     * {@link ArchRuleDefinition#all(ClassesTransformer) all(customObjects)}.{@link GivenObjects#should(ArchCondition) should(behaveAsExpected())}
     * </code>
     *
     * @return An {@link ArchRule} which can be evaluated on imported {@link JavaClasses}
     */
    @PublicAPI(usage = ACCESS)
    ArchRule should(ArchCondition<? super T> condition);

    /**
     * Allows to restrict the set of objects under consideration. E.g.
     * <br><br>
     * <code>
     * {@link ArchRuleDefinition#all(ClassesTransformer) all(customObjects)}.{@link GivenObjects#that(DescribedPredicate) that(predicate)}
     * </code>
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    GivenConjunction<T> that(DescribedPredicate<? super T> predicate);
}
