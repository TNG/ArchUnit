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
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Allows to join together any existing {@link ArchCondition} of this rule with another {@link ArchCondition}
 * via {@link #andShould() and} or {@link #orShould() or}. Note that the behavior is always fully left-associative, i.e. there is no
 * "operator precedence" as for {@code &&} or {@code ||}. Take for example
 *
 * <pre><code>
 * classes()...should().bePublic().orShould().bePrivate().andShould().haveNameMatching(pattern)
 * </code></pre>
 *
 * The semantics of the new condition will be {@code (public OR private) AND haveNameMatching}
 * – and not {@code public || (private && haveNameMatching)}.
 * <br><br>
 * Thus, for more complex conditions please consider explicitly joining {@link ArchConditions preconfigured conditions}
 * or custom {@link ArchCondition conditions} via {@link ArchCondition#and(ArchCondition)} and {@link ArchCondition#or(ArchCondition)}
 * where you can freely control the precedence via nesting. E.g.
 *
 * <pre><code>
 * classes()...should(
 *   bePublic().or(
 *     bePrivate().and(haveNameMatching(pattern))
 *   )
 * )
 * </code></pre>
 *
 * Note that inverting the rule, e.g. via {@link ArchRuleDefinition#noClasses()}, will also invert the join operator. I.e.
 * if you define {@code noClasses().should().a().orShould().b()} it will mean that all classes must satisfy {@code not(a) AND not(b)}.
 */
public interface ClassesShouldConjunction extends ArchRule {

    /**
     * Joins another condition to this rule with {@code and} semantics. That is, all classes under test
     * now needs to satisfy the existing condition and this new one.<br>
     * Note that this is always left-associative and does not support any operator
     * precedence, see {@link ClassesShouldConjunction}.
     *
     * @param condition Another condition to be 'and'-ed to the current condition of this rule
     * @return A syntax element, which can be used to further restrict the classes under consideration
     */
    @PublicAPI(usage = ACCESS)
    ClassesShouldConjunction andShould(ArchCondition<? super JavaClass> condition);

    /**
     * Like {@link #andShould(ArchCondition)} but offers a fluent API to pick the condition to join.
     */
    @PublicAPI(usage = ACCESS)
    ClassesShould andShould();

    /**
     * Joins another condition to this rule with {@code or} semantics. That is, all classes under test
     * now needs to satisfy the existing condition or this given one.<br>
     * Note that this is always left-associative and does not support any operator
     * precedence, see {@link ClassesShouldConjunction}.
     *
     * @param condition Another condition to be 'and'-ed to the current condition of this rule
     * @return A syntax element, which can be used to further restrict the classes under consideration
     */
    @PublicAPI(usage = ACCESS)
    ClassesShouldConjunction orShould(ArchCondition<? super JavaClass> condition);

    /**
     * Like {@link #orShould(ArchCondition)} but offers a fluent API to pick the condition to join.
     */
    @PublicAPI(usage = ACCESS)
    ClassesShould orShould();
}
