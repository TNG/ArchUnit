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
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface GivenClasses extends GivenObjects<JavaClass> {

    /**
     * Allows to restrict the set of classes under consideration. E.g.
     * <br><br>
     * <code>
     * {@link ArchRuleDefinition#classes() classes()}.{@link GivenClasses#that() that()}.{@link ClassesThat#haveSimpleName(String) haveSimpleName("Example")}
     * </code>
     *
     * @return A syntax element, which can be used to restrict the classes under consideration
     */
    @PublicAPI(usage = ACCESS)
    ClassesThat<GivenClassesConjunction> that();

    /**
     * Allows to restrict the set of classes under consideration. E.g.
     * <br><br>
     * <code>
     * {@link ArchRuleDefinition#classes() classes()}.{@link GivenClasses#that(DescribedPredicate) that(haveSimpleName("Example"))}
     * </code>
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @Override
    @PublicAPI(usage = ACCESS)
    GivenClassesConjunction that(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Allows to specify assertions for the set of classes under consideration. E.g.
     * <br><br>
     * <code>
     * {@link ArchRuleDefinition#classes() classes()}.{@link GivenClasses#should() should()}.{@link ClassesShould#haveSimpleName(String) haveSimpleName("Example")}
     * </code>
     *
     * @return A syntax element, which can be used to restrict the classes under consideration
     */
    @PublicAPI(usage = ACCESS)
    ClassesShould should();

    /**
     * Allows to specify assertions for the set of classes under consideration. E.g.
     * <br><br>
     * <code>
     * {@link ArchRuleDefinition#classes() classes()}.{@link GivenClasses#should(ArchCondition) should(haveSimpleName("Example"))}
     * </code>
     *
     * @return A syntax element, which can be used to restrict the classes under consideration
     */
    @Override
    @PublicAPI(usage = ACCESS)
    ClassesShouldConjunction should(ArchCondition<? super JavaClass> condition);
}
