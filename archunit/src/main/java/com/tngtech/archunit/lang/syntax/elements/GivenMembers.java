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
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface GivenMembers<MEMBER extends JavaMember> extends GivenObjects<MEMBER> {

    /**
     * Allows to restrict the set of members under consideration. E.g.
     * <br><br>
     * <code>
     * {@link ArchRuleDefinition#members() members()}.{@link GivenMembers#that() that()}.{@link MembersThat#haveName(String) haveName("foo")}
     * </code>
     *
     * @return A syntax element, which can be used to restrict the members under consideration
     */
    @PublicAPI(usage = ACCESS)
    MembersThat<? extends GivenMembersConjunction<MEMBER>> that();

    /**
     * Allows to restrict the set of members under consideration. E.g.
     * <br><br>
     * <code>
     * {@link ArchRuleDefinition#members() members()}.{@link GivenMembers#that(DescribedPredicate) that(haveName("foo"))}
     * </code>
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @Override
    @PublicAPI(usage = ACCESS)
    GivenMembersConjunction<MEMBER> that(DescribedPredicate<? super MEMBER> predicate);

    /**
     * Allows to specify assertions for the set of members under consideration. E.g.
     * <br><br>
     * <code>
     * {@link ArchRuleDefinition#members() members()}.{@link GivenMembers#should() should()}.{@link MembersShould#haveName(String) haveName("foo")}
     * </code>
     *
     * @return A syntax element, which can be used to restrict the members under consideration
     */
    @PublicAPI(usage = ACCESS)
    MembersShould<? extends MembersShouldConjunction<MEMBER>> should();

    /**
     * Allows to specify assertions for the set of members under consideration. E.g.
     * <br><br>
     * <code>
     * {@link ArchRuleDefinition#members() members()}.{@link GivenMembers#should(ArchCondition) should(haveName("foo"))}
     * </code>
     *
     * @return A syntax element, which can be used to restrict the members under consideration
     */
    @Override
    @PublicAPI(usage = ACCESS)
    MembersShouldConjunction<MEMBER> should(ArchCondition<? super MEMBER> condition);
}
